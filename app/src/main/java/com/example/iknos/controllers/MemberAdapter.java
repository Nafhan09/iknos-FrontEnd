package com.example.iknos.controllers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.iknos.R;
import com.example.iknos.models.RoomDetailResponse;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private final List<RoomDetailResponse.RoomMember> memberList;
    private final Context context;

    // Konstruktor untuk inisialisasi MemberAdapter
    // Konstruktor meliputi inisialisasi context dan daftar member (memberList)
    public MemberAdapter(Context context, List<RoomDetailResponse.RoomMember> memberList) {
        this.context = context;
        this.memberList = memberList;
    }

    @NonNull
    @Override
    // Method untuk membuat ViewHolder baru
    // Method meliputi proses inflate layout item_member menjadi View
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    // Method untuk memasang data ke komponen UI di dalam item_member
    // Method meliputi pengaturan teks username dan pemuatan foto profil (avatar) menggunakan Glide
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        RoomDetailResponse.RoomMember member = memberList.get(position);

        if (member.user != null) {
            holder.tvMemberUsername.setText(member.user.username);

            if (member.user.avatarUrl != null && !member.user.avatarUrl.isEmpty()) {
                Glide.with(context)
                        .load(member.user.avatarUrl)
                        .placeholder(R.mipmap.ic_launcher_round)
                        .into(holder.ivMemberAvatar);
            } else {
                holder.ivMemberAvatar.setImageResource(R.mipmap.ic_launcher_round);
            }
        }
    }

    @Override
    // Method untuk mendapatkan total jumlah member
    // Method ini mengembalikan ukuran atau jumlah elemen di dalam list memberList
    public int getItemCount() {
        return memberList.size();
    }

    // Kelas ViewHolder untuk caching komponen UI dari item_member
    // Kelas ini meliputi referensi ke komponen avatar dan username agar lebih efisien
    static class MemberViewHolder extends RecyclerView.ViewHolder {

        CircleImageView ivMemberAvatar;
        TextView tvMemberUsername;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMemberAvatar = itemView.findViewById(R.id.ivMemberAvatar);
            tvMemberUsername = itemView.findViewById(R.id.tvMemberUsername);
        }
    }
}
