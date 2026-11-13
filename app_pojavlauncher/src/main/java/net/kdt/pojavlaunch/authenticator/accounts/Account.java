
package net.kdt.pojavlaunch.authenticator.accounts;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import net.kdt.pojavlaunch.*;
import net.kdt.pojavlaunch.authenticator.AuthType;
import net.kdt.pojavlaunch.utils.FileUtils;
import net.kdt.pojavlaunch.utils.JSONUtils;

import java.io.*;
import java.net.URL;

import androidx.annotation.Keep;

import com.google.gson.JsonParseException;

import org.apache.commons.io.IOUtils;

@Keep
public class Account {
    public transient File mSaveLocation;

    public String accessToken = "0";
    public String profileId = "00000000-0000-0000-0000-000000000000";
    public String username = "Steve";
    public AuthType authType = AuthType.LOCAL;
    public boolean isMicrosoft = false;
    public String refreshToken = "0";
    public String xuid;
    public long expiresAt;

    private transient Bitmap mFaceCache;

    protected Account() {}

    public void updateSkinFace() {
        String skinFaceUrlTemplate = authType.skinUrl;
        if (skinFaceUrlTemplate == null) return;

        String skinFaceUrl = String.format(skinFaceUrlTemplate, username);

        try {
            Log.i("SkinLoader", "Updating skin face...");

            File skinFile = getSkinFaceFile();

            byte[] skinBytes = IOUtils.toByteArray(new URL(skinFaceUrl));
            Bitmap skinBitmap = BitmapFactory.decodeByteArray(
                skinBytes, 0, skinBytes.length
            );

            if (skinBitmap == null) return;

            Bitmap skinFace = new SkinHeadRenderer().render(100, skinBitmap);
            skinBitmap.recycle();

            if (skinFace == null) return;

            try (FileOutputStream output = new FileOutputStream(skinFile)) {
                skinFace.compress(Bitmap.CompressFormat.WEBP, 90, output);
            }

            if (mFaceCache != null) {
                mFaceCache.recycle();
            }

            mFaceCache = skinFace;

            Log.i("SkinLoader", "Update skin face success");

        } catch (IOException e) {
            Log.w("SkinLoader", "Could not update skin face", e);
        }
    }

    public boolean isLocal() {
        return accessToken.equals("0");
    }

    public void save() throws IOException {
        FileUtils.ensureParentDirectory(mSaveLocation);
        JSONUtils.writeToFile(mSaveLocation, this);
    }

    public Account reload() {
        try {
            Account account = JSONUtils.readFromFile(
                mSaveLocation, Account.class
            );

            if (account == null) return null;

            account.mSaveLocation = mSaveLocation;
            return account;

        } catch (IOException | JsonParseException e) {
            return null;
        }
    }

    /**
     * Saves a custom Minecraft avatar generated from an imported skin.
     */
    public void setCustomSkinFace(Bitmap skinFace) throws IOException {
        if (skinFace == null) {
            throw new IOException("Skin avatar is empty");
        }

        File avatarFile = getCustomSkinFaceFile();
        FileUtils.ensureParentDirectory(avatarFile);

        try (FileOutputStream output = new FileOutputStream(avatarFile)) {
            boolean success = skinFace.compress(
                Bitmap.CompressFormat.WEBP, 95, output
            );

            if (!success) {
                throw new IOException("Could not save custom avatar");
            }
        }

        if (mFaceCache != null && mFaceCache != skinFace) {
            mFaceCache.recycle();
        }

        mFaceCache = skinFace;

        Log.i("SkinLoader", "Custom avatar saved");
    }

    /**
     * Returns the custom avatar first, then the online account avatar.
     */
    public Bitmap getSkinFace() {
        if (mFaceCache != null && !mFaceCache.isRecycled()) {
            return mFaceCache;
        }

        File customAvatar = getCustomSkinFaceFile();

        if (customAvatar.exists()) {
            mFaceCache = BitmapFactory.decodeFile(
                customAvatar.getAbsolutePath()
            );

            if (mFaceCache != null) {
                return mFaceCache;
            }
        }

        if (isLocal()) {
            return null;
        }

        File onlineAvatar = getSkinFaceFile();

        if (!onlineAvatar.exists()) {
            return null;
        }

        mFaceCache = BitmapFactory.decodeFile(
            onlineAvatar.getAbsolutePath()
        );

        return mFaceCache;
    }

    /**
     * Each account gets its own custom avatar file.
     */
    private File getCustomSkinFaceFile() {
        String accountId;

        if (mSaveLocation != null) {
            accountId = mSaveLocation.getName();
        } else {
            accountId = profileId + "-" + authType.name();
        }

        return new File(
            Tools.DIR_CACHE,
            "custom-skin-face-" + accountId + ".webp"
        );
    }

    /**
     * Existing online skin avatar storage.
     */
    private File getSkinFaceFile() {
        return new File(
            Tools.DIR_CACHE,
            "skin-face-" + profileId + "-" + authType.name() + ".webp"
        );
    }
}
