Tutorial Guide: Override Attachment options with Location option and create a custom message template for it
________________________________________
Task Overview
We will:
1.	Override the default attachment options in CometChat UI
2.	Add a custom “Send Location” option
3.	Use Google Static Maps API to generate a map preview
4.	Create and register a location_card custom message
5.	Display the custom message with a proper layout using a CometChatMessageTemplate
________________________________________
 Pre-requisites
1. Permissions (Add in AndroidManifest.xml):

        <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
________________________________________
2. Add Google Maps Static API Key
Create a key at: https://console.cloud.google.com/apis/library/static-maps-backend.googleapis.com
Replace YOUR_API_KEY in the code with your actual Google Static Maps API key.
________________________________________
 FILE 1: MessageActivity.java
 Purpose:
This is the screen that handles chats. You’ll:
•	Add location button in the composer
•	Define how to render the custom message
________________________________________
 Step-by-Step Changes
🔸 1. Add required imports at the top:

    import android.Manifest;
    import android.content.pm.PackageManager;
    import android.location.Location;
    import com.google.android.gms.location.FusedLocationProviderClient;
    import com.google.android.gms.location.LocationServices;
________________________________________
🔸 2. Declare global variables inside your MessageActivity:

    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST = 100;
________________________________________
🔸 3. Initialize location services in onCreate():

    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
________________________________________
🔸 4. Inside your onSuccess() for user or group, call:

    setupLocationFeature();
________________________________________
🔸 5. Add the setupLocationFeature() method:

    private void setupLocationFeature() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST);
            return;
        }

    // Add custom attachment action
    List<CometChatMessageComposerAction> actions = new ArrayList<>();
    CometChatMessageComposerAction locationAction = new CometChatMessageComposerAction();
    locationAction.setTitle("Send Location");
    locationAction.setIcon(R.drawable.ic_location); // Use your custom location icon

    locationAction.setOnClick(() -> {
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

    // Custom template registration
    List<CometChatMessageTemplate> templates = ChatConfigurator
            .getDataSource()
            .getMessageTemplates(messageList.getAdditionParameter());

    CometChatMessageTemplate locationTemplate = new CometChatMessageTemplate();
    locationTemplate.setType("location_card");
    locationTemplate.setCategory(UIKitConstants.MessageCategory.CUSTOM);

    // Add custom content view to avoid default bubble
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
________________________________________
🔸 6. Add the sendLocationMessage() method:

    private void sendLocationMessage(Location location) {
        String lat = String.valueOf(location.getLatitude());
        String lng = String.valueOf(location.getLongitude());

    String mapUrl = "https://maps.googleapis.com/maps/api/staticmap?" +
            "center=" + lat + "," + lng +
            "&zoom=15&size=600x300&markers=color:red|" + lat + "," + lng +
            "&key=YOUR_API_KEY"; // Replace with real key

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
________________________________________
🗂️ FILE 2: res/layout/location_card.xml
Create this file for rendering your custom location message.

    <?xml version="1.0" encoding="utf-8"?>
    <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:minWidth="180dp"
        android:maxWidth="260dp"
        android:orientation="vertical"
        android:background="@drawable/bg_outgoing_bubble"
        android:padding="8dp">

    <ImageView
        android:id="@+id/mapPreview"
        android:layout_width="match_parent"
        android:layout_height="160dp"
        android:scaleType="centerCrop"
        android:contentDescription="Map preview" />
</LinearLayout>
________________________________________
🗂️ FILE 3: res/drawable/bg_outgoing_bubble.xml

    <shape xmlns:android="http://schemas.android.com/apk/res/android"
        android:shape="rectangle">
        <solid android:color="#6E5DC6" /> <!-- Violet background -->
        <corners android:radius="16dp" />
    </shape>
________________________________________
🗂️ FILE 4: res/drawable/ic_location.xml
Use any vector asset for location icon (can be downloaded from Material Icons).
________________________________________
📌 Final Output
•	✅ A location icon is shown in the attachment menu.
•	✅ Tapping it sends a Google Maps static preview to the chat.
•	✅ The custom location message shows in a colored bubble with proper size.
•	✅ No duplicate time or default bubbles.

