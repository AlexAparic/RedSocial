package com.example.redsocial;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.redsocial.adapters.CommentAdapter;
import com.example.redsocial.models.Comment;
import com.example.redsocial.providers.AuthProvider;
import com.example.redsocial.providers.CommentsProvider;
import com.example.redsocial.providers.LikesProvider;
import com.example.redsocial.providers.NotificationProvider;
import com.example.redsocial.providers.TokenProvider;
import com.example.redsocial.utils.RelativeTime;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.example.redsocial.adapters.SliderAdapter;
import com.example.redsocial.models.SliderItem;
import com.example.redsocial.providers.PostProvider;
import com.example.redsocial.providers.UsersProvider;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.smarteist.autoimageslider.IndicatorAnimations;
import com.smarteist.autoimageslider.SliderAnimations;
import com.smarteist.autoimageslider.SliderView;
import com.squareup.picasso.Picasso;
import com.example.redsocial.models.FCMBody;
import com.example.redsocial.models.FCMResponse;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostDetailActivity extends AppCompatActivity {

    SliderView mSliderView;
    SliderAdapter mSliderAdapter;
    List<SliderItem> mSliderItems = new ArrayList<>();
    PostProvider mPostProvider;
    UsersProvider mUsersProvider;

    String mExtraPostId;

    TextView mTextViewTitle;
    TextView mTextViewDescription;
    TextView mTextViewUsername;
    TextView mTextViewPhone;
    TextView mTextViewNameCategory;
    ImageView mImageViewCategory;
    CircleImageView mCircleImageViewProfile;
    Button mButtonShowProfile;

    FloatingActionButton mFabComment;
    String mIdUser = "";
    CommentsProvider mCommentsProvider;
    AuthProvider mAuthProvider;
    RecyclerView mRecyclerView;
    CommentAdapter mAdapter;
    TextView mTextViewRelativeTime;
    TextView mTextViewLikes;
    LikesProvider mLikesProvider;
    Toolbar mToolbar;
    TokenProvider mTokenProvider;
    NotificationProvider mNotificationProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        mSliderView = findViewById(R.id.imageSlider);
        mTextViewTitle = findViewById(R.id.textViewTitle);
        mTextViewDescription = findViewById(R.id.textViewDescription);
        mTextViewUsername = findViewById(R.id.textViewUsername);
        mTextViewPhone = findViewById(R.id.textViewPhone);
        mTextViewNameCategory = findViewById(R.id.textViewNameCategory);
        mImageViewCategory = findViewById(R.id.imageViewCategory);
        mCircleImageViewProfile = findViewById(R.id.circleImageProfile);
        mButtonShowProfile = findViewById(R.id.btnShowProfile);

        mFabComment = findViewById(R.id.fabComment);
        mCommentsProvider = new CommentsProvider();
        mRecyclerView = findViewById(R.id.recyclerViewComments);
        mTextViewRelativeTime = findViewById(R.id.textViewRelativeTime);
        mTextViewLikes = findViewById(R.id.textViewLikes);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(PostDetailActivity.this);
        mRecyclerView.setLayoutManager(linearLayoutManager);

        mToolbar = findViewById(R.id.toolbar);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        mPostProvider = new PostProvider();
        mUsersProvider = new UsersProvider();
        mAuthProvider = new AuthProvider();
        mLikesProvider = new LikesProvider();
        mTokenProvider = new TokenProvider();
        mNotificationProvider = new NotificationProvider();
        mTokenProvider = new TokenProvider();

        mExtraPostId = getIntent().getStringExtra("id");



        mFabComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialogComment();
            }
        });




        mButtonShowProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToShowProfile();
            }
        });

        getPost();

        getNumberLikes();
    }

    private void getNumberLikes() {
        mLikesProvider.getLikesByPost(mExtraPostId).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                int numberLikes = queryDocumentSnapshots.size();
                if (numberLikes == 1) {
                    mTextViewLikes.setText(numberLikes + " Me gusta");
                }
                else {
                    mTextViewLikes.setText(numberLikes + " Me gustas");
                }
            }
        });

    }
    @Override
    protected void onStart() {
        super.onStart();

        Query query = mCommentsProvider.getCommentsByPost(mExtraPostId);
        FirestoreRecyclerOptions<Comment> options =
                new FirestoreRecyclerOptions.Builder<Comment>()
                        .setQuery(query, Comment.class)
                        .build();
        mAdapter = new CommentAdapter(options, PostDetailActivity.this);
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.startListening();
    }
    @Override
    protected void onStop() {
        super.onStop();
        mAdapter.stopListening();
    }

    private void showDialogComment() {
        AlertDialog.Builder alert = new AlertDialog.Builder(PostDetailActivity.this);
        alert.setTitle("¡COMENTARIO!");
        alert.setMessage("Ingresa tu comentario");

        final EditText editText = new EditText(PostDetailActivity.this);
        editText.setHint("Texto");


        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(36, 0, 36, 36);
        editText.setLayoutParams(params);
        RelativeLayout container = new RelativeLayout(PostDetailActivity.this);
        RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        container.setLayoutParams(relativeParams);
        container.addView(editText);

        alert.setView(container);

        alert.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String value = editText.getText().toString();
                if (!value.isEmpty()) {
                    createComment(value);
                }
                else {
                    Toast.makeText(PostDetailActivity.this, "Debe ingresar el comentario", Toast.LENGTH_SHORT).show();
                }
            }
        });
        alert.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        alert.show();
    }

    private void createComment(final String value) {
        Comment comment = new Comment();
        comment.setComment(value);
        comment.setIdPost(mExtraPostId);
        comment.setIdUser(mAuthProvider.getUid());
        comment.setTimestamp(new Date().getTime());
        mCommentsProvider.create(comment).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                     sendNotification(value);
                    Toast.makeText(PostDetailActivity.this, "El comentario se creo correctamente", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(PostDetailActivity.this, "No se pudo crear el comentario", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void sendNotification(final String comment) {
        if (mIdUser == null) {
            return;
        }
        mTokenProvider.getToken(mIdUser).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    if (documentSnapshot.contains("token")) {
                        String token = documentSnapshot.getString("token");
                        Map<String, String> data = new HashMap<>();
                        data.put("title", "NUEVO COMENTARIO");
                        data.put("body", comment);
                        FCMBody body = new FCMBody(token, "high", "4500s", data);
                        mNotificationProvider.sendNotification(body).enqueue(new Callback<FCMResponse>() {
                            @Override
                            public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                                if (response.body() != null) {
                                    if (response.body().getSuccess() == 1) {
                                        Toast.makeText(PostDetailActivity.this, "La notificacion se envio correcatemente", Toast.LENGTH_SHORT).show();
                                    }
                                    else {
                                        Toast.makeText(PostDetailActivity.this, "La notificacion no se pudo enviar", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                else {
                                    Toast.makeText(PostDetailActivity.this, "La notificacion no se pudo enviar", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<FCMResponse> call, Throwable t) {

                            }
                        });
                    }
                }
                else {
                    Toast.makeText(PostDetailActivity.this, "El token de notificaciones del usuario no existe", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void goToShowProfile() {
        if (!mIdUser.equals("")) {
            Intent intent = new Intent(PostDetailActivity.this, UserProfileActivity.class);
            intent.putExtra("idUser", mIdUser);
            startActivity(intent);
        }
        else {
            Toast.makeText(this, "El id del usuario aun no se carga", Toast.LENGTH_SHORT).show();
        }
    }

    private void instanceSlider() {
        mSliderAdapter = new SliderAdapter(PostDetailActivity.this, mSliderItems);
        mSliderView.setSliderAdapter(mSliderAdapter);
        mSliderView.setIndicatorAnimation(IndicatorAnimations.THIN_WORM);
        mSliderView.setSliderTransformAnimation(SliderAnimations.SIMPLETRANSFORMATION);
        mSliderView.setAutoCycleDirection(SliderView.AUTO_CYCLE_DIRECTION_RIGHT);
        mSliderView.setIndicatorSelectedColor(Color.WHITE);
        mSliderView.setIndicatorUnselectedColor(Color.GRAY);
        mSliderView.setScrollTimeInSec(5);
        mSliderView.setAutoCycle(true);
        mSliderView.startAutoCycle();
    }

    private void getPost() {
        mPostProvider.getPostById(mExtraPostId).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {

                    if (documentSnapshot.contains("image1")) {
                        String image1 = documentSnapshot.getString("image1");
                        SliderItem item = new SliderItem();
                        item.setImageUrl(image1);
                        mSliderItems.add(item);
                    }
                    if (documentSnapshot.contains("title")) {
                        String title = documentSnapshot.getString("title");
                        mTextViewTitle.setText(title.toUpperCase());
                    }
                    if (documentSnapshot.contains("description")) {
                        String description = documentSnapshot.getString("description");
                        mTextViewDescription.setText(description);
                    }
                    if (documentSnapshot.contains("category")) {
                        String category = documentSnapshot.getString("category");
                        mTextViewNameCategory.setText(category);

                        if (category.equals("DEPORTE")) {
                            mImageViewCategory.setImageResource(R.drawable.deporte);
                        }
                        else if (category.equals("COCINA")) {
                            mImageViewCategory.setImageResource(R.drawable.cocina);
                        }
                        else if (category.equals("NOTICIAS")) {
                            mImageViewCategory.setImageResource(R.drawable.noticias);
                        }
                        else if (category.equals("MEMES")) {
                            mImageViewCategory.setImageResource(R.drawable.memes);
                        }
                    }
                    if (documentSnapshot.contains("idUser")) {
                        mIdUser = documentSnapshot.getString("idUser");
                        getUserInfo(mIdUser);
                    }
                    if (documentSnapshot.contains("timestamp")) {
                        long timestamp = documentSnapshot.getLong("timestamp");
                        String relativeTime = RelativeTime.getTimeAgo(timestamp, PostDetailActivity.this);
                        mTextViewRelativeTime.setText(relativeTime);
                    }
                    instanceSlider();
                }
            }
        });
    }

    private void getUserInfo(String idUser) {
        mUsersProvider.getUser(idUser).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    if (documentSnapshot.contains("username")) {
                        String username = documentSnapshot.getString("username");
                        mTextViewUsername.setText(username);
                    }
                    if (documentSnapshot.contains("phone")) {
                        String phone = documentSnapshot.getString("phone");
                        mTextViewPhone.setText(phone);
                    }
                    if (documentSnapshot.contains("image_profile")) {
                        String imageProfile = documentSnapshot.getString("image_profile");
                        Picasso.with(PostDetailActivity.this).load(imageProfile).into(mCircleImageViewProfile);
                    }
                }
            }
        });
    }
}