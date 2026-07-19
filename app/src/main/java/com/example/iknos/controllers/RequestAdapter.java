package com.example.iknos.controllers;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.text.Html;
import androidx.core.text.HtmlCompat;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iknos.R;
import com.example.iknos.models.JoinRequestModel;

import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    private final List<JoinRequestModel> requestList;
    private final RoomActivity activity;
    private final String roomName;

    public RequestAdapter(RoomActivity activity, List<JoinRequestModel> requestList, String roomName) {
        this.activity = activity;
        this.requestList = requestList;
        this.roomName = roomName;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_request, parent, false);

        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {

        JoinRequestModel request = requestList.get(position);

        holder.tvUsername.setText(request.getUser().getUsername());

        String htmlText = "Pada Ruangan <b>" + roomName + "</b>";
        holder.tvReqDescription.setText(HtmlCompat.fromHtml(htmlText, HtmlCompat.FROM_HTML_MODE_LEGACY));

        holder.btnAccept.setOnClickListener(v -> {

            activity.approveOrRejectUser(
                    request.getId(),
                    "approve",
                    request.getRoomId(),
                    roomName
            );

        });

        holder.btnDecline.setOnClickListener(v -> {

            activity.approveOrRejectUser(
                    request.getId(),
                    "reject",
                    request.getRoomId(),
                    roomName
            );

        });

    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {

        TextView tvUsername;
        TextView tvReqDescription;
        ImageButton btnAccept, btnDecline;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);

            tvUsername = itemView.findViewById(R.id.tvReqUsername);
            tvReqDescription = itemView.findViewById(R.id.tvReqDescription);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
        }
    }
}