package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.chatapp.adapters.ChatAdapter;
import com.example.chatapp.models.Chats;
import com.example.chatapp.models.Users;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageActivity extends AppCompatActivity {

    private TextView username;
    private CircleImageView userProfile, userStatus;
    private EditText textSend;
    private FloatingActionButton btnSend;
    private RecyclerView recyclerView;

    private FirebaseUser fUser;
    private DatabaseReference reference;

    private Intent intent;
    private String userId;

    private ChatAdapter adapter;
    private List<Chats> mChats;

    private ValueEventListener seenListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
       // getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        username = findViewById(R.id.message_username);
        userProfile = findViewById(R.id.message_profile_image);
        textSend = findViewById(R.id.text_send);
        btnSend = findViewById(R.id.send);
        userStatus = findViewById(R.id.message_status);
        recyclerView = findViewById(R.id.message_rec_view);

        recyclerView.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);

        intent = getIntent();
        userId = intent.getStringExtra("userId");

        fUser = FirebaseAuth.getInstance().getCurrentUser();

        assert userId != null;
        reference = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Users user = snapshot.getValue(Users.class);
                assert user != null;
                username.setText(user.getUsername());
                if(user.getImageUrl().equals("default")){
                    userProfile.setImageResource(R.drawable.avatar);
                }else{
                    Glide.with(getApplicationContext())
                            .load(user.getImageUrl())
                            .into(userProfile);
                }
                if(user.getStatus().equals("online")){
                    userStatus.setVisibility(View.VISIBLE);
                }else{
                    userStatus.setVisibility(View.GONE);
                }

                readMessage(fUser.getUid(), userId, user.getImageUrl());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        if(recyclerView != null){
            seenMessage(userId);
        }

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = textSend.getText().toString();
                if(!message.equals(" ")){
                    sendMessage(fUser.getUid(), userId, message);
                }
                textSend.setText("");
            }
        });

    }

    private void seenMessage(final String userId){
        reference = FirebaseDatabase.getInstance().getReference("Chats");
        seenListener = reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Chats chats = snapshot.getValue(Chats.class);
                    assert chats != null;
                    if (chats.getReceiver().equals(fUser.getUid()) && chats.getSender().equals(userId)) {
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("isSeen", true);
                        snapshot.getRef().updateChildren(map);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendMessage(String sender, String receiver, String message){
        reference = FirebaseDatabase.getInstance().getReference("Chats");

        HashMap<String, Object> map = new HashMap<>();
        map.put("sender", sender);
        map.put("receiver", receiver);
        map.put("message", message);
        map.put("isSeen", false);

        reference.push().setValue(map);

        final DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("ChatList")
                .child(fUser.getUid())
                .child(userId);

        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(!snapshot.exists()){
                    chatRef.child("id").setValue(userId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void readMessage(final String myId, final String userId, final String imageUrl){
        mChats = new ArrayList<>();
        reference = FirebaseDatabase.getInstance().getReference("Chats");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mChats.clear();
                for(DataSnapshot dataSnapshot: snapshot.getChildren()){
                    Chats chat = dataSnapshot.getValue(Chats.class);
                    assert chat != null;
                    if(chat.getSender().equals(myId) && chat.getReceiver().equals(userId)
                    || chat.getSender().equals(userId) && chat.getReceiver().equals(myId)){
                        mChats.add(chat);
                    }
                }
                adapter = new ChatAdapter(MessageActivity.this, mChats, imageUrl);
                recyclerView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void checkStatus(String status){
        fUser = FirebaseAuth.getInstance().getCurrentUser();

        reference = FirebaseDatabase.getInstance().getReference("Users").child(fUser.getUid());

        HashMap<String, Object> map = new HashMap<>();
        map.put("status", status);
        reference.updateChildren(map);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkStatus("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        reference.removeEventListener(seenListener);
        checkStatus("offline");
    }

}