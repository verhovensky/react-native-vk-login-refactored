package camp.kuznetsov.rn.vkontakte;


import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import com.facebook.react.bridge.*;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.vk.api.sdk.*;
import com.vk.api.sdk.auth.VKAccessToken;
import com.vk.api.sdk.auth.VKAuthCallback;
import com.vk.api.sdk.exceptions.VKApiCodes;
import com.vk.api.sdk.utils.VKUtils;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

enum class KEYS {
    ACCESS_TOKEN("access_token"),
    EXPIRES_IN("expires_in"),
    USER_ID("user_id"),
    SECRET("secret"),
    EMAIL("email"),
}

@ReactModule(name="VKAuthModule")
public class VKAuthModule extends ReactContextBaseJavaModule implements ActivityEventListener {
    private static final String E_ACTIVITY_DOES_NOT_EXIST = "E_ACTIVITY_DOES_NOT_EXIST";
    private static final String E_NOT_INITIALIZED = "E_NOT_INITIALIZED";
    private static final String E_FINGERPRINTS_ERROR = "E_FINGERPRINTS_ERROR";
    private static final String TOKEN_INVALID = "TOKEN_INVALID";
    private static final String M_NOT_INITIALIZED = "VK SDK must be initialized first";
    private static final String E_VK_UNKNOWN = "E_VK_UNKNOWN";
    private static final String	E_VK_API_ERROR = "E_VK_API_ERROR";
    private static final String E_VK_CANCELED = "E_VK_CANCELED";
    private static final String E_VK_JSON_FAILED = "E_VK_JSON_FAILED";
    private static final String E_VK_REQUEST_HTTP_FAILED = "E_VK_REQUEST_HTTP_FAILED";
    private static final String E_VK_REQUEST_NOT_PREPARED = "E_VK_REQUEST_NOT_PREPARED";

    private Promise loginPromise;
    private boolean isInitialized = false;

    @Override
    public void onNewIntent(Intent intent) {}

    public VKAuthModule(final ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addActivityEventListener(this);

        VK.initialize(reactContext);
        isInitialized = true;
    }

    @Override
    public String getName() {
        return "VkontakteManager";
    }

    @Nullable
    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put(TOKEN_INVALID, TOKEN_INVALID);
        return constants;
    }

    @ReactMethod
    public void initialize(final Integer appId){
        // not working due to VK API
        
        // if (appId != 0) {
        //     VK.initialize(getReactApplicationContext());
        //     isInitialized = true;
        // }
        // else {
        //     throw new JSApplicationIllegalArgumentException("VK App Id cannot be 0");
        // }
    }
    
    @ReactMethod
    public void initialized(final Promise promise){
        promise.resolve(isInitialized);
    }

    @ReactMethod
    public void login(final ReadableArray scope, final Promise promise) {
        if (!isInitialized) {
            promise.reject(E_NOT_INITIALIZED, M_NOT_INITIALIZED);
            return;
        }
        Activity activity = getCurrentActivity();

        if (activity == null) {
            promise.reject(E_ACTIVITY_DOES_NOT_EXIST, "Activity doesn't exist");
            return;
        }

        int scopeSize = scope == null ? 0 : scope.size();
        // String[] scopeArray = new String[scopeSize];

        Collection<VKScope> scopeArray = new HashSet<>();
        for (int i = 0; i < scopeSize; i++) {
            scopeArray.add(scope.getString(i));
        }

        if (VK.isLoggedIn()) {
            VK.logout();
        }

        loginPromise = promise;
        VK.login(activity, scopeArray);
    }

    @ReactMethod
    public void logout(Promise promise) {
        if (!isInitialized) {
            promise.reject(E_NOT_INITIALIZED, M_NOT_INITIALIZED);
            return;
        }
        VK.logout();
        promise.resolve(null);
    }

    @ReactMethod
    public void isLoggedIn(Promise promise) {
        if (isInitialized) {
            promise.resolve(VK.isLoggedIn());
        }
        else {
            promise.reject(E_NOT_INITIALIZED, M_NOT_INITIALIZED);
        }
    }

    @ReactMethod
    public void getAccessToken(Promise promise) {
        // promise.resolve(serializeAccessToken(VKAccessToken.get()));
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        VK.onActivityResult(requestCode, resultCode, data, new VKAuthCallback() {
            @Override
            public void onLogin(VKAccessToken res) {
                if (loginPromise != null) {
                    loginPromise.resolve(serializeAccessToken(res));
                    loginPromise = null;
                }
            }

            @Override
            public void onLoginFailed(int errorCode) {
                if (loginPromise != null) {
                    rejectPromiseWithVKError(loginPromise, error);
                    loginPromise = null;
                }
            }
        });
    }

    private void rejectPromiseWithVKError(Promise promise, int errorCode) {
        switch (errorCode) {
            case VKAuthCallback.AUTH_CANCELED:
                promise.reject(E_VK_CANCELED, 'User canceled');
                break;
            default:
                promise.reject(E_VK_UNKNOWN, 'Unknown error');
                break;
        }
    }

    @ReactMethod
    public void getCertificateFingerprint(Promise promise) {
        try {
            ReactApplicationContext reactContext = getReactApplicationContext();
            String[] fingerprints = VKUtils.getCertificateFingerprint(reactContext, reactContext.getPackageName());
            WritableArray result = Arguments.fromArray(fingerprints);
            promise.resolve(result);
        } catch (Exception e) {
            promise.reject(E_FINGERPRINTS_ERROR, e.toString());
        }
    }

    private @Nullable WritableMap serializeAccessToken(@Nullable VKAccessToken token){
        if (token == null) {
            return null;
        }

        WritableMap result = Arguments.createMap();

        result.putString(KEYS.ACCESS_TOKEN, token.accessToken);
        result.putInt(KEYS.EXPIRES_IN, token.expiresIn);
        result.putString(KEYS.USER_ID, token.userId);
        result.putString(KEYS.SECRET, token.secret);
        result.putString(KEYS.EMAIL, token.email);

        return result;
    }
}
