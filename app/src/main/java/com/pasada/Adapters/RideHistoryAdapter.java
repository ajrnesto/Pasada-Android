package com.pasada.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pasada.Objects.Destination;
import com.pasada.Objects.Distance;
import com.pasada.Objects.Duration;
import com.pasada.Objects.ModesOfTransport;
import com.pasada.Objects.RideRequest;
import com.pasada.Objects.UserLocation;
import com.pasada.R;
import com.pasada.Utils.Utils;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Objects;

public class RideHistoryAdapter extends RecyclerView.Adapter<RideHistoryAdapter.rideHistoryViewHolder> {

    private static final FirebaseFirestore DB = FirebaseFirestore.getInstance();

    Context context;
    ArrayList<RideRequest> arrRideHistory;

    public RideHistoryAdapter(Context context, ArrayList<RideRequest> arrRideHistory) {
        this.context = context;
        this.arrRideHistory = arrRideHistory;
    }

    @NonNull
    @Override
    public rideHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.cardview_ride_history, parent, false);
        return new rideHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull rideHistoryViewHolder holder, int position) {
        RideRequest rideHistory = arrRideHistory.get(position);

        String uid = rideHistory.getUid();
        String userUid = rideHistory.getUserUid();
        String riderUid = rideHistory.getRiderUid();
        Destination destination = rideHistory.getDestination();
        Distance distance = rideHistory.getDistance();
        Duration duration = rideHistory.getDuration();
        UserLocation userLocation = rideHistory.getUserLocation();
        String userFullName = rideHistory.getUserFullName();
        ModesOfTransport modesOfTransport = rideHistory.getModesOfTransport();
        long timestampStart = rideHistory.getTimestampStart();
        long timestampEnd = rideHistory.getTimestampEnd();
        String status = rideHistory.getStatus();

        holder.tvRide.setText(destination.getName());
        holder.tvDistance.setText("Distance: " + distance.getHumanReadable());DB.collection("rates").document("rates")
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            double minimumFare = task.getResult().getDouble("minimumFare");
                            double extraFarePerKm = task.getResult().getDouble("extraFarePerKm");

                            DecimalFormat fareFormat = new DecimalFormat("#.##");
                            holder.tvFare.setText("Fare: ₱" + fareFormat.format(Utils.calculateFare(distance.getInMeters(), minimumFare, extraFarePerKm)));
                        }
                    }
                });
        loadTimestamp(holder, timestampEnd);
        loadStatus(holder, status);
    }

    private void loadTimestamp(rideHistoryViewHolder holder, long timestamp) {
        SimpleDateFormat sdfTimestamp = new SimpleDateFormat("MMM dd");
        holder.tvTimestamp.setText(sdfTimestamp.format(timestamp));
    }

    private void loadStatus(rideHistoryViewHolder holder, String status) {
        if (Objects.equals(status, "cancelled")) {
            holder.tvStatusMessage.setVisibility(View.VISIBLE);
        }
        else {
            holder.tvStatusMessage.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return arrRideHistory.size();
    }

    public class rideHistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvStatusMessage, tvRide, tvTimestamp, tvDistance, tvFare;

        public rideHistoryViewHolder(@NonNull View itemView) {
            super(itemView);

            tvStatusMessage = itemView.findViewById(R.id.tvStatusMessage);
            tvRide = itemView.findViewById(R.id.tvRide);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvFare = itemView.findViewById(R.id.tvFare);
        }
    }
}
