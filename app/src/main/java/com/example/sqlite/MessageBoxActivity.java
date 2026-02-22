package com.example.sqlite;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MessageBoxActivity extends AppCompatActivity {

    private RecyclerView rvMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_box);

        Toolbar toolbar = findViewById(R.id.toolbar_message_box);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvMessages = findViewById(R.id.rv_messages);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));

        // Dữ liệu mẫu Gen Z cực chất
        List<Message> messageList = new ArrayList<>();
        messageList.add(new Message("Chào mừng Gen Z! 🚀", 
            "Chào mừng bạn đến với Smart Wallet. Bắt đầu hành trình làm chủ ví tiền ngay thôi nào!", "Vừa xong"));
        messageList.add(new Message("Bí kíp ăn ngon vẫn dư tiền 🍜", 
            "Mẹo: Nấu cơm tại nhà giúp bạn tiết kiệm 30% chi phí ăn uống đấy. Thử ngay nhé!", "1 giờ trước"));
        messageList.add(new Message("Sao lưu thành công! ✅", 
            "Dữ liệu của bạn đã được đóng gói an toàn. Đừng quên cất file .db cẩn thận nhé!", "Hôm qua"));
        messageList.add(new Message("Cảnh báo 'bay màu' 💸", 
            "Ví của bạn đang có dấu hiệu tiêu quá tay. Hãy kiểm tra lại mục Ăn uống ngay!", "2 ngày trước"));

        MessageAdapter adapter = new MessageAdapter(messageList);
        rvMessages.setAdapter(adapter);
    }

    public static class Message {
        String title, content, time;
        public Message(String title, String content, String time) {
            this.title = title; this.content = content; this.time = time;
        }
    }

    private static class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
        private final List<Message> list;
        public MessageAdapter(List<Message> list) { this.list = list; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Message m = list.get(position);
            holder.tvTitle.setText(m.title);
            holder.tvContent.setText(m.content);
            holder.tvTime.setText(m.time);
        }

        @Override public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvContent, tvTime;
            public ViewHolder(View v) { super(v);
                tvTitle = v.findViewById(R.id.tv_message_title);
                tvContent = v.findViewById(R.id.tv_message_content);
                tvTime = v.findViewById(R.id.tv_message_time);
            }
        }
    }
}