
package net.kdt.pojavlaunch;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.kdt.pojavlaunch.authenticator.accounts.Account;
import net.kdt.pojavlaunch.authenticator.accounts.Accounts;
import net.kdt.pojavlaunch.authenticator.accounts.SkinHeadRenderer;

import com.greecroafto.crystal.R;

import java.io.IOException;
import java.io.InputStream;

public class AccountManagerActivity extends Activity {

    private static final int REQUEST_PICK_SKIN = 4102;

    private ImageView avatarView;
    private TextView usernameView;
    private TextView avatarHintView;

    private static final int PINK = 0xFFFF4FD8;
    private static final int CARD = 0xFF1A0C1E;
    private static final int WHITE = 0xFFFFFFFF;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_manager);

        avatarView = findViewById(R.id.avatar);
        usernameView = findViewById(R.id.account_username);
        avatarHintView = findViewById(R.id.avatar_hint);

        Button addAccount = findViewById(R.id.button_add_account);
        LinearLayout avatarCard = findViewById(R.id.avatar_card);

        styleCard(avatarCard);
        styleButton(addAccount);

        addAccount.setOnClickListener(v -> {
            LauncherActivity.OPEN_LOGIN_AFTER_START = true;

            Intent intent = new Intent(
                AccountManagerActivity.this,
                LauncherActivity.class
            );

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        avatarCard.setOnClickListener(v -> openSkinPicker());

        refreshAccount();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAccount();
    }

    private void refreshAccount() {
        Account account = Accounts.getCurrent();

        if (account == null) {
            usernameView.setText("No account selected");
            avatarView.setImageResource(android.R.drawable.sym_def_app_icon);
            avatarHintView.setText("Add an account first");
            return;
        }

        usernameView.setText(account.username);

        Bitmap face = account.getSkinFace();

        if (face != null && !face.isRecycled()) {
            avatarView.setImageBitmap(face);
            avatarHintView.setText("Tap to change your avatar");
        } else {
            avatarView.setImageResource(android.R.drawable.sym_def_app_icon);
            avatarHintView.setText("Tap to import a Minecraft skin PNG");
        }
    }

    private void openSkinPicker() {
        Account account = Accounts.getCurrent();

        if (account == null) {
            Toast.makeText(
                this,
                "Add or select an account first",
                Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/png");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivityForResult(intent, REQUEST_PICK_SKIN);
    }

    @Override
    protected void onActivityResult(
        int requestCode,
        int resultCode,
        Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQUEST_PICK_SKIN ||
            resultCode != RESULT_OK ||
            data == null ||
            data.getData() == null) {
            return;
        }

        importSkin(data.getData());
    }

    private void importSkin(Uri uri) {
        Bitmap skinBitmap = null;
        Bitmap faceBitmap = null;

        try (InputStream input = getContentResolver().openInputStream(uri)) {

            if (input == null) {
                throw new IOException("Could not open the selected image");
            }

            skinBitmap = BitmapFactory.decodeStream(input);

            if (skinBitmap == null) {
                throw new IOException("Invalid PNG image");
            }

            int width = skinBitmap.getWidth();
            int height = skinBitmap.getHeight();

            if (width != height ||
                width < 64 ||
                width % 64 != 0 ||
                width > 1024) {

                throw new IOException(
                    "Use a square Minecraft skin PNG, such as 64x64 or 128x128"
                );
            }

            faceBitmap = new SkinHeadRenderer().render(100, skinBitmap);

            if (faceBitmap == null) {
                throw new IOException("Could not render this Minecraft skin");
            }

            Account account = Accounts.getCurrent();

            if (account == null) {
                throw new IOException("No account is currently selected");
            }

            account.setCustomSkinFace(faceBitmap);

            avatarView.setImageBitmap(faceBitmap);
            avatarHintView.setText("Tap to change your avatar");

            Toast.makeText(
                this,
                "Crystal avatar updated!",
                Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {
            if (faceBitmap != null && !faceBitmap.isRecycled()) {
                faceBitmap.recycle();
            }

            Toast.makeText(
                this,
                e.getMessage() == null
                    ? "Could not import this skin"
                    : e.getMessage(),
                Toast.LENGTH_LONG
            ).show();

        } finally {
            if (skinBitmap != null && !skinBitmap.isRecycled()) {
                skinBitmap.recycle();
            }
        }
    }

    private void styleCard(View view) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(CARD);
        background.setCornerRadius(dp(20));
        background.setStroke(dp(1), 0x66FF4FD8);
        view.setBackground(background);
    }

    private void styleButton(Button button) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(PINK);
        background.setCornerRadius(dp(16));
        background.setStroke(dp(1), 0xFFFF8AE8);

        button.setBackground(background);
        button.setTextColor(WHITE);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setElevation(dp(4));
    }

    private int dp(float value) {
        return (int) (
            value * getResources().getDisplayMetrics().density + 0.5f
        );
    }
}
