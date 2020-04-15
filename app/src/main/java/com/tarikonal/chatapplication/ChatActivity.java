package com.tarikonal.chatapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.onesignal.OneSignal;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.Reference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    RecyclerView recyclerView;
    RecyclerViewAdapter recyclerViewAdapter;
    EditText messageText;
    FirebaseDatabase database;
    DatabaseReference databaseReference;

    private ArrayList<String> chatMessages = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        //chatMessages.add("Merhaba");
        //chatMessages.add("Napıyon la");
        messageText = findViewById(R.id.chat_activity_message_text);
        recyclerView = findViewById(R.id.recycler_view);
        recyclerViewAdapter = new RecyclerViewAdapter(chatMessages);
        RecyclerView.LayoutManager  recyclerViewManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(recyclerViewManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(recyclerViewAdapter);
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference();

        getData();

        // OneSignal Initialization
        OneSignal.startInit(this)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init();
        OneSignal.idsAvailable(new OneSignal.IdsAvailableHandler() {
            @Override
            public void idsAvailable(final String userId, String registrationId) {
                System.out.println("User ID:"+userId);
               final UUID uuid = UUID.randomUUID();
                DatabaseReference newReference = database.getReference("PlayerIDs");
                newReference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<String> playerIDsFromServer = new ArrayList<>();

                        for(DataSnapshot ds : dataSnapshot.getChildren()){
                            HashMap<String,String> hashMap = (HashMap<String,String>) ds.getValue();
                            String currentPlayerID =  hashMap.get("playerID");
                            playerIDsFromServer.add(currentPlayerID);
                        }
                        if(!playerIDsFromServer.contains(userId)){
                            databaseReference.child("PlayerIDs").child(uuid.toString()).child("playerID").setValue(userId);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });



            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.option_menu,menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.options_menu_sign_out){
            mAuth.signOut();
            Intent intent = new Intent(getApplicationContext(),SignUpActivity.class);
            startActivity(intent);

        } else if(item.getItemId() == R.id.options_menu_profile){
            Intent intent = new Intent(getApplicationContext(),ProfileActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    public void sendMessage(View view) {
        UUID uuid = UUID.randomUUID();
        String uuidString = uuid.toString();
        FirebaseUser user = mAuth.getCurrentUser();
        //SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        //String currentDateandTime = sdf.format(new Date());
        final String messageToSend = messageText.getText().toString();
        databaseReference.child("Chats").child(uuidString).child("usermessage").setValue(messageToSend);
        databaseReference.child("Chats").child(uuidString).child("useremail").setValue(user.getEmail().toString());
        databaseReference.child("Chats").child(uuidString).child("datetime").setValue(ServerValue.TIMESTAMP);

        messageText.setText("");

        //onesignal
        DatabaseReference newReference = database.getReference("PlayerIDs");
        newReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    HashMap<String,String> hashMap = (HashMap<String,String>) ds.getValue();
                    String playerID = hashMap.get("playerID");
                    System.out.println("Player ID Server:"+playerID);
                    try {
                        OneSignal.postNotification(new JSONObject("{'contents': {'en':'"+messageToSend+"'}, 'include_player_ids': ['" + playerID + "']}"), null);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    public String getDatafTimeStamp(long timestamp){
        java.util.Date time=new java.util.Date(timestamp*1000);
        SimpleDateFormat pre = new SimpleDateFormat("EEE MM dd HH:mm:ss zzz yyyy");
        //Hear Define your returning date formate
        return pre.format(time);
    }
    public void getData(){
        DatabaseReference newReference = database.getReference("Chats");
        Query query = newReference.orderByChild("datetime");
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //System.out.println("dataSnapShot Children:"+dataSnapshot.getChildren());
                //System.out.println("dataSnapShot Value:"+dataSnapshot.getValue());
                //System.out.println("dataSnapShot Key:"+dataSnapshot.getKey());
                chatMessages.clear();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    //System.out.println("data value="+ds.getValue());
                    HashMap<String,String> hashMap = ( HashMap<String,String>) ds.getValue();
                    String userEmail= hashMap.get("useremail");
                    String usermessage= hashMap.get("usermessage");
                    //String datetime= hashMap.get("datetime");

                    //chatMessages.add("["+datetime.toString()+"]:"+usermessage);
                    chatMessages.add(userEmail+ " : "+ usermessage);
                    recyclerViewAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ChatActivity.this, databaseError.getMessage().toString(), Toast.LENGTH_LONG).show();

            }
        });
    }
}
