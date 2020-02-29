package com.teachassist.teachassist;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class LaunchActivity extends AppCompatActivity {
    String username;
    String password;
    boolean RemeberMe;
    public static final String CREDENTIALS = "credentials";
    public static final String USERNAME = "USERNAME";
    public static final String PASSWORD = "PASSWORD";
    public static final String REMEMBERME = "REMEMBERME";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Open file with username and password
        SharedPreferences sharedPreferences = getSharedPreferences(CREDENTIALS, MODE_PRIVATE);
        username = sharedPreferences.getString(USERNAME, "");
        password = sharedPreferences.getString(PASSWORD, "");
        RemeberMe = sharedPreferences.getBoolean(REMEMBERME, false);
        //ReactInstanceManagerSingleton.initializeInstance(getApplicationContext(), getApplication()).getReactInstanceManager();




        if(!username.isEmpty() && !password.isEmpty() && RemeberMe) {

            final SharedPreferences sharedPreferencesNotifications = getSharedPreferences("notifications", MODE_PRIVATE);
            String token = sharedPreferencesNotifications.getString("token", "");
            //register with firebase, if user is already registered nothing will happen
            try {
                final FirebaseAuth auth = FirebaseAuth.getInstance();
                FirebaseUser currentUser = auth.getCurrentUser();
                if(currentUser == null){
                    auth.signInWithEmailAndPassword(username+"@"+password+".android", password)
                            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (!task.isSuccessful()) {
                                        System.out.println("Does Not Exist");
                                        auth.createUserWithEmailAndPassword(username+"@"+password+".android", password);
                                    }else{
                                        System.out.println("ALREADY LOGGED IN");
                                    }
                                }
                            });

                }else {
                    if (!token.equals("") && !currentUser.getDisplayName().equals(token)) {
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(token)
                                .build();
                        currentUser.updateProfile(profileUpdates);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            Intent myIntent = new Intent(LaunchActivity.this, MainActivity.class);
            myIntent.putExtra("username", username);
            myIntent.putExtra("password", password);
            startActivity(myIntent);

        }
        else{
            Intent myIntent = new Intent(LaunchActivity.this, login.class);
            startActivity(myIntent);
        }
        finish();
    }
}
