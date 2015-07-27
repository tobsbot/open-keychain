/*
 * Copyright (C) 2015 Vincent Breitmoser <v.breitmoser@mugenguild.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.ui;


import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.provider.KeychainContract.Keys;


public class SubkeyStatusList extends LinearLayout implements LoaderCallbacks<Cursor> {

    public static final String[] PROJECTION = new String[] {
            Keys.KEY_ID,
            Keys.CREATION,
            Keys.CAN_CERTIFY,
            Keys.CAN_SIGN,
            Keys.CAN_ENCRYPT,
            Keys.HAS_SECRET,
            Keys.EXPIRY,
            Keys.IS_REVOKED
    };
    public static final int INDEX_KEY_ID = 0;
    public static final int INDEX_CREATION = 1;
    public static final int INDEX_CAN_CERTIFY = 2;
    public static final int INDEX_CAN_SIGN = 3;
    public static final int INDEX_CAN_ENCRYPT = 4;
    public static final int INDEX_HAS_SECRET = 5;
    public static final int INDEX_EXPIRY = 6;
    public static final int INDEX_IS_REVOKED = 7;

    public static final String ARG_MASTER_KEY_ID = "master_key_id";

    TextView vCertText, vSignText, vDecryptText;
    ImageView vCertIcon, vSignIcon, vDecryptIcon;
    View vCertYubi, vSignYubi, vDecryptYubi;

    public SubkeyStatusList(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOrientation(VERTICAL);

        View view = LayoutInflater.from(context).inflate(R.layout.subkey_status_card_content, this, true);

        vCertText = (TextView) view.findViewById(R.id.cap_cert_text);
        vSignText = (TextView) view.findViewById(R.id.cap_sign_text);
        vDecryptText = (TextView) view.findViewById(R.id.cap_decrypt_text);

        vCertIcon = (ImageView) view.findViewById(R.id.cap_cert_icon);
        vSignIcon = (ImageView) view.findViewById(R.id.cap_sign_icon);
        vDecryptIcon = (ImageView) view.findViewById(R.id.cap_decrypt_icon);

        vCertYubi = view.findViewById(R.id.cap_cert_yubi);
        vSignYubi = view.findViewById(R.id.cap_sign_yubi);
        vDecryptYubi = view.findViewById(R.id.cap_decrypt_yubi);

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        long masterKeyId = args.getLong(ARG_MASTER_KEY_ID);
        return new CursorLoader(getContext(),
                Keys.buildKeysUri(masterKeyId), PROJECTION, null, null, null);
    }

    // this is just a list of statuses a key can be in, which we can also display
    enum KeyDisplayStatus {
        OK (R.color.android_green_light, R.color.primary_text,
                R.string.cap_cert_ok, R.string.cap_sign_ok, R.string.cap_decrypt_ok),
        DIVERT (R.color.android_green_light, R.color.primary_text,
                R.string.cap_cert_divert, R.string.cap_sign_divert, R.string.cap_decrypt_divert),
        REVOKED (R.color.android_red_light, R.color.android_red_light,
                R.string.cap_cert_revoked, R.string.cap_sign_revoked, R.string.cap_decrypt_revoked),
        EXPIRED (R.color.android_red_light, R.color.android_red_light,
                R.string.cap_cert_expired, R.string.cap_sign_expired, R.string.cap_decrypt_expired),
        UNAVAILABLE (R.color.android_red_light, R.color.android_red_light,
                R.string.cap_cert_unavailable, R.string.cap_sign_unavailable, R.string.cap_decrypt_unavailable);

        @ColorRes final int mColor, mTextColor;
        @StringRes final int mCertifyStr, mSignStr, mEncryptStr;

        KeyDisplayStatus(@ColorRes int color, @ColorRes int textColor,
                @StringRes int certifyStr, @StringRes int signStr, @StringRes int encryptStr) {
            mColor = color;
            mTextColor = textColor;
            mCertifyStr = certifyStr;
            mSignStr = signStr;
            mEncryptStr = encryptStr;
        }

    }

    @SuppressWarnings("deprecation")
    @Override
    public void onLoadFinished(Loader loader, Cursor cursor) {

        SubKeyItem bestCertify = findBestKeyOfType(cursor, INDEX_CAN_CERTIFY);
        if (bestCertify == null) {
            vCertIcon.setColorFilter(getResources().getColor(R.color.android_red_light));
            vCertText.setText(R.string.cap_cert_null);
            vCertText.setTextColor(getResources().getColor(R.color.primary_text));
            vCertYubi.setVisibility(View.GONE);
        } else {
            KeyDisplayStatus certStatus = bestCertify.getStatus();
            vCertIcon.setColorFilter(getResources().getColor(certStatus.mColor));
            vCertText.setText(certStatus.mCertifyStr);
            vCertText.setTextColor(getResources().getColor(certStatus.mTextColor));
            vCertYubi.setVisibility(
                    bestCertify.mSecretKeyType == SecretKeyType.DIVERT_TO_CARD ? View.VISIBLE : View.GONE);
        }

        SubKeyItem bestSign = findBestKeyOfType(cursor, INDEX_CAN_SIGN);
        if (bestSign == null) {
            vSignIcon.setColorFilter(getResources().getColor(R.color.android_red_light));
            vSignText.setText(R.string.cap_sign_null);
            vSignText.setTextColor(getResources().getColor(R.color.primary_text));
            vSignYubi.setVisibility(View.GONE);
        } else {
            KeyDisplayStatus signStatus = bestSign.getStatus();
            vSignIcon.setColorFilter(getResources().getColor(signStatus.mColor));
            vSignText.setText(signStatus.mSignStr);
            vSignText.setTextColor(getResources().getColor(signStatus.mTextColor));
            vSignYubi.setVisibility(
                    bestSign.mSecretKeyType == SecretKeyType.DIVERT_TO_CARD ? View.VISIBLE : View.GONE);
        }

        SubKeyItem bestEncrypt = findBestKeyOfType(cursor, INDEX_CAN_ENCRYPT);
        if (bestEncrypt == null) {
            vDecryptIcon.setColorFilter(getResources().getColor(R.color.android_red_light));
            vDecryptText.setText(R.string.cap_decrypt_null);
            vDecryptText.setTextColor(getResources().getColor(R.color.primary_text));
            vDecryptYubi.setVisibility(View.GONE);
        } else {
            KeyDisplayStatus decryptStatus = bestEncrypt.getStatus();
            vDecryptIcon.setColorFilter(getResources().getColor(decryptStatus.mColor));
            vDecryptText.setText(decryptStatus.mEncryptStr);
            vDecryptText.setTextColor(getResources().getColor(decryptStatus.mTextColor));
            vDecryptYubi.setVisibility(
                    bestEncrypt.mSecretKeyType == SecretKeyType.DIVERT_TO_CARD ? View.VISIBLE : View.GONE);
        }

    }

    @Override
    public void onLoaderReset(Loader loader) {

    }

    @Nullable
    private SubKeyItem findBestKeyOfType(Cursor cursor, int type) {
        if (!cursor.moveToFirst()) {
            return null;
        }
        SubKeyItem result = null;
        do {
            if (cursor.getInt(type) == 0) {
                continue;
            }

            SubKeyItem subKey = new SubKeyItem(cursor);
            // if there is no "good" candidate, take it.
            if (result == null || subKey.compareTo(result) < 0) {
                result = subKey;
            }

        } while (cursor.moveToNext());
        return result;
    }

    static class SubKeyItem implements Comparable<SubKeyItem> {
        final int mPosition;
        final long mKeyId;
        final Date mCreation;
        final SecretKeyType mSecretKeyType;
        final boolean mIsRevoked, mIsExpired;
        final boolean mCanCertify, mCanSign, mCanEncrypt;

        SubKeyItem(Cursor cursor) {

            mPosition = cursor.getPosition();

            mKeyId = cursor.getLong(INDEX_KEY_ID);
            mCreation = new Date(cursor.getLong(INDEX_CREATION) * 1000);

            mSecretKeyType = SecretKeyType.fromNum(cursor.getInt(INDEX_HAS_SECRET));

            mIsRevoked = cursor.getInt(INDEX_IS_REVOKED) > 0;
            Date expiryDate = null;
            if (!cursor.isNull(INDEX_EXPIRY)) {
                expiryDate = new Date(cursor.getLong(INDEX_EXPIRY) * 1000);
            }
            mIsExpired = expiryDate != null && expiryDate.before(new Date());

            mCanCertify = cursor.getInt(INDEX_CAN_CERTIFY) > 0;
            mCanSign = cursor.getInt(INDEX_CAN_SIGN) > 0;
            mCanEncrypt = cursor.getInt(INDEX_CAN_ENCRYPT) > 0;

        }

        KeyDisplayStatus getStatus() {
            if (isValid()) {
                if (mSecretKeyType == SecretKeyType.DIVERT_TO_CARD) {
                    return KeyDisplayStatus.DIVERT;
                }
                return KeyDisplayStatus.OK;

            } else {
                if (mIsRevoked) {
                    return KeyDisplayStatus.REVOKED;
                }
                if (mIsExpired) {
                    return KeyDisplayStatus.EXPIRED;
                }
                return KeyDisplayStatus.UNAVAILABLE;
            }
        }

        boolean newerThan(SubKeyItem other) {
            return mCreation.after(other.mCreation);
        }

        boolean isValid() {
            return mSecretKeyType.isUsable() && !mIsRevoked && !mIsExpired;
        }

        @Override
        public int compareTo(@NonNull SubKeyItem another) {
            // if one is valid and the other isn't, the valid one always comes first
            if (isValid() ^ another.isValid()) {
                return isValid() ? -1 : 1;
            }
            // if the secret key types are different, the "stronger one" one wins
            if (mSecretKeyType != another.mSecretKeyType) {
                return 1;
            }
            // compare usability, if one is "more usable" than the other, that one comes first
            int usability = mSecretKeyType.compareUsability(another.mSecretKeyType);
            if (usability != 0) {
                return usability;
            }
            // otherwise, the newer one comes first
            return newerThan(another) ? -1 : 1;
        }
    }

}
