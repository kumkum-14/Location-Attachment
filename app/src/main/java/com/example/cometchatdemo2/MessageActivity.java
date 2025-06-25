package com.example.cometchatdemo2;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cometchat.chat.constants.CometChatConstants;
import com.cometchat.chat.core.CometChat;
import com.cometchat.chat.exceptions.CometChatException;
import com.cometchat.chat.models.BaseMessage;
import com.cometchat.chat.models.CustomMessage;
import com.cometchat.chat.models.Group;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.messagecomposer.CometChatMessageComposer;
import com.cometchat.chatuikit.messageheader.CometChatMessageHeader;
import com.cometchat.chatuikit.messagelist.CometChatMessageList;
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.framework.ChatConfigurator;
import com.cometchat.chatuikit.shared.models.CometChatMessageComposerAction;
import com.cometchat.chatuikit.shared.models.CometChatMessageTemplate;
import com.cometchat.chatuikit.shared.viewholders.MessagesViewHolderListener;
import com.cometchat.chatuikit.shared.views.messagebubble.CometChatMessageBubble;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MessageActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 100;

    private CometChatMessageHeader messageHeader;
    private CometChatMessageList messageList;
    private CometChatMessageComposer messageComposer;

    private FusedLocationProviderClient fusedLocationClient;
    private User chatUser;
    private Group chatGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        messageHeader = findViewById(R.id.message_header);
        messageList = findViewById(R.id.message_list);
        messageComposer = findViewById(R.id.message_composer);

        messageHeader.setOnBackButtonPressed(() -> finish());

        String uid = getIntent().getStringExtra("uid");
        String guid = getIntent().getStringExtra("guid");

        if (uid != null) {
            CometChat.getUser(uid, new CometChat.CallbackListener<User>() {
                @Override
                public void onSuccess(User user) {
                    chatUser = user;
                    messageHeader.setUser(user);
                    messageList.setUser(user);
                    messageComposer.setUser(user);
                    setupLocationFeature();
                }

                @Override
                public void onError(CometChatException e) {
                    finish();
                }
            });
        } else if (guid != null) {
            CometChat.getGroup(guid, new CometChat.CallbackListener<Group>() {
                @Override
                public void onSuccess(Group group) {
                    chatGroup = group;
                    messageHeader.setGroup(group);
                    messageList.setGroup(group);
                    messageComposer.setGroup(group);
                    setupLocationFeature();
                }

                @Override
                public void onError(CometChatException e) {
                    finish();
                }
            });
        } else {
            Toast.makeText(this, "Missing chat ID", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupLocationFeature() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;
        }

        List<CometChatMessageComposerAction> actions = new ArrayList<>();
        CometChatMessageComposerAction locationAction = new CometChatMessageComposerAction();
        locationAction.setTitle("Send Location");
        locationAction.setIcon(R.drawable.ic_location);

        locationAction.setOnClick(() -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
                return;
            }

            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    sendLocationMessage(location);
                } else {
                    Toast.makeText(this, "Could not get location", Toast.LENGTH_SHORT).show();
                }
            });
        });

        actions.add(locationAction);
        messageComposer.setAttachmentOptions(actions);

        List<CometChatMessageTemplate> templates = ChatConfigurator
                .getDataSource()
                .getMessageTemplates(messageList.getAdditionParameter());

        CometChatMessageTemplate locationTemplate = new CometChatMessageTemplate();
        locationTemplate.setType("location_card");
        locationTemplate.setCategory(UIKitConstants.MessageCategory.CUSTOM);

        // ✅ Using setContentView to avoid outer bubble
        locationTemplate.setContentView(new MessagesViewHolderListener() {
            @Override
            public View createView(Context context, CometChatMessageBubble bubble, UIKitConstants.MessageBubbleAlignment alignment) {
                return LayoutInflater.from(context).inflate(R.layout.location_card, null);
            }

            @Override
            public void bindView(Context context, View view, BaseMessage message,
                                 UIKitConstants.MessageBubbleAlignment alignment,
                                 RecyclerView.ViewHolder holder,
                                 List<BaseMessage> messages, int pos) {
                try {
                    CustomMessage msg = (CustomMessage) message;
                    JSONObject data = msg.getCustomData();
                    String mapUrl = data.optString("map_url");

                    ImageView mapImage = view.findViewById(R.id.mapPreview);

                    Glide.with(context)
                            .load(mapUrl)
                            .placeholder(R.drawable.mp)
                            .error(R.drawable.mp)
                            .into(mapImage);


                    mapImage.setOnClickListener(v -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + mapUrl));
                        intent.setPackage("com.google.android.apps.maps");
                        context.startActivity(intent);
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        templates.add(locationTemplate);
        messageList.setTemplates(templates);
    }

    private void sendLocationMessage(Location location) {
        String lat = String.valueOf(location.getLatitude());
        String lng = String.valueOf(location.getLongitude());

        String mapUrl = "https://maps.googleapis.com/maps/api/staticmap?" +
                "center=" + lat + "," + lng +
                "&zoom=15&size=600x300&markers=color:red|" + lat + "," + lng +
                " API KEY "; // Replace with your actual key

        JSONObject data = new JSONObject();
        try {
            data.put("map_url", mapUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }

        CustomMessage locationMessage = new CustomMessage(
                chatUser != null ? chatUser.getUid() : chatGroup.getGuid(),
                chatUser != null ? CometChatConstants.RECEIVER_TYPE_USER : CometChatConstants.RECEIVER_TYPE_GROUP,
                "location_card",
                data
        );

        CometChatUIKit.sendCustomMessage(locationMessage, null);
    }
}
