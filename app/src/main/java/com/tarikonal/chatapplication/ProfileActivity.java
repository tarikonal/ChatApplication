package com.tarikonal.chatapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class ProfileActivity extends AppCompatActivity {
    EditText ageText;
    ImageView userImageView;
    Uri selected;
    private FirebaseDatabase database;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ageText = findViewById(R.id.ageText);
        userImageView=findViewById(R.id.userImageView);

        database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference();
        storageReference = FirebaseStorage.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        getData();
    }

    public void selectPicture(View view){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},1);
        }else {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent,2);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==1){
            if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent,2);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode==2 && resultCode==RESULT_OK && data!=null){
            //Kullanıcı resim seçtiyse
            selected = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),selected);
                userImageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void getData(){
            DatabaseReference newReference = database.getReference("Profiles");
            newReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        HashMap<String,String> hashMap = (HashMap<String,String>) ds.getValue();
                        String userName = hashMap.get("useremail");
                        //System.out.println( "username:"+userName);
                        if(userName.matches(mAuth.getCurrentUser().getEmail().toString())){
                            String userAge = hashMap.get("userage");
                            String userImageURL = hashMap.get("userimageurl");
                            if(userAge!=null && userImageURL!=null){
                                ageText.setText(userAge);
                                Picasso.get().load(userImageURL).into(userImageView);
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
    }

    public void upload(View view) {
        final UUID uuid = UUID.randomUUID();
        String imageName = "images/"+uuid+".jpg";
        StorageReference newReference = storageReference.child(imageName);
        newReference.putFile(selected).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                StorageReference profileImageRef = FirebaseStorage.getInstance().getReference("images/"+uuid+".jpg");
                profileImageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        String downloadURL = uri.toString();
                        //System.out.println("download URL: "+downloadURL);
                        UUID uuidProfile = UUID.randomUUID();
                        String userAge = ageText.getText().toString();
                        FirebaseUser user = mAuth.getCurrentUser();
                        String userEmail = user.getEmail().toString();

                        databaseReference.child("Profiles").child(uuidProfile.toString()).child("userimageurl").setValue(downloadURL);
                        databaseReference.child("Profiles").child(uuidProfile.toString()).child("userage").setValue(userAge);
                        databaseReference.child("Profiles").child(uuidProfile.toString()).child("useremail").setValue(userEmail);

                        Toast.makeText(ProfileActivity.this, "Uploaded!", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(getApplicationContext(),ChatActivity.class);
                        startActivity(intent);
                    }
                });

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(ProfileActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
