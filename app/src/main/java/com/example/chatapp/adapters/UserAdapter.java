package com.example.chatapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.MessageActivity;
import com.example.chatapp.R;
import com.example.chatapp.models.Chats;
import com.example.chatapp.models.Users;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    Context mContext;
    List<Users> usersList;
    boolean isOnline;

    String theLastMessage;

    public UserAdapter(Context mContext, List<Users> users, boolean isOnline) {
        this.mContext = mContext;
        this.usersList = users;
        this.isOnline = isOnline;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(isOnline){
            View view = LayoutInflater.from(mContext).inflate(R.layout.chat_item, parent, false);
            return new ViewHolder(view);
        }else{
            View view = LayoutInflater.from(mContext).inflate(R.layout.user_item, parent, false);
            return new ViewHolder(view);
        }

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Users users = usersList.get(position);
        holder.username.setText(users.getUsername());

        if(users.getImageUrl().equals("default")){
            holder.userImage.setImageResource(R.drawable.avatar);
        }else{
            Glide.with(mContext)
                    .load(users.getImageUrl())
                    .into(holder.userImage);
        }

        if(isOnline){
            if(users.getStatus().equals("online")){
                holder.status.setVisibility(View.VISIBLE);
            }else{
                holder.status.setVisibility(View.GONE);
            }
        }else {
            holder.status.setVisibility(View.GONE);
        }

        if(isOnline){
            lastMessage(users.getId(), holder.lastMessage);
        }
//        else {
//            holder.lastMessage.setVisibility(View.GONE);
//        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, MessageActivity.class);
                intent.putExtra("userId", users.getId());
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        CircleImageView userImage;
        TextView username;
        CircleImageView status;
        TextView lastMessage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userImage = itemView.findViewById(R.id.user_profile_image);
            username = itemView.findViewById(R.id.user_name);
            status = itemView.findViewById(R.id.status_online);
            lastMessage = itemView.findViewById(R.id.last_message);
        }
    }

    private void lastMessage(final String userId, final TextView lastMsg){
        theLastMessage = "default";

        final FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Chats");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot dataSnapshot: snapshot.getChildren()){
                    Chats chats = dataSnapshot.getValue(Chats.class);
                    assert chats != null;
                    assert fUser != null;
                    if(chats.getReceiver().equals(fUser.getUid()) && chats.getSender().equals(userId)
                    || chats.getSender().equals(fUser.getUid()) && chats.getReceiver().equals(userId)){
                        theLastMessage = chats.getMessage();
                    }
                }

                switch (theLastMessage){
                    case "default":
                        lastMsg.setText(R.string.no_message);
                        break;

                    default:
                        lastMsg.setText(theLastMessage);
                        break;
                }

                theLastMessage = "default";
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}
