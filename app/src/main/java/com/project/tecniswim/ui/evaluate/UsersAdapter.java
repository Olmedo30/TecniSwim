package com.project.tecniswim.ui.evaluate;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.project.tecniswim.R;
import java.util.ArrayList;
import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(UserItem user);
    }

    private final List<UserItem> originalList;
    private final List<UserItem> filteredList;
    private OnItemClickListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public UsersAdapter(List<UserItem> users) {
        this.originalList = users;
        this.filteredList = new ArrayList<>(users);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void updateList(List<UserItem> newList) {
        originalList.clear();
        originalList.addAll(newList);
        filter(""); // actualizar filteredList
    }

    public void filter(String query) {
        filteredList.clear();
        for (UserItem user : originalList) {
            String fullName = (user.getFirstName() + " " + user.getLastName()).toLowerCase();
            String email = user.getEmail().toLowerCase();
            if (fullName.contains(query.toLowerCase()) || email.contains(query.toLowerCase())) {
                filteredList.add(user);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemV = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(itemV);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserItem user = filteredList.get(position);
        holder.bind(user, position);
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName  = itemView.findViewById(R.id.tvUserName);
            tvEmail = itemView.findViewById(R.id.tvUserEmail);

            itemView.setOnClickListener(v -> {
                int prevPos = selectedPosition;
                selectedPosition = getAdapterPosition();
                notifyItemChanged(prevPos);
                notifyItemChanged(selectedPosition);

                if (listener != null) {
                    listener.onItemClick(filteredList.get(selectedPosition));
                }
            });
        }

        void bind(UserItem user, int position) {
            String fullName = user.getFirstName() + " " + user.getLastName();
            tvName.setText(fullName);
            tvEmail.setText(user.getEmail());

            // Animación o marca para el elemento seleccionado
            if (position == selectedPosition) {
                itemView.setBackgroundColor(0x8034A853); // semitransparente verde
            } else {
                itemView.setBackgroundColor(Color.TRANSPARENT);
            }
        }
    }
}
