package lucasfeitosa.tech.easytracking.util;

import android.text.TextUtils;
import android.util.Base64;

import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.schedulers.Schedulers;

public class RestClient {
    private static final String BASE_URL = "http://lucasfeitosa.online/";


    private static RetrofitInterface REST_CLIENT;
    private static Retrofit RETROFIT;
    private static String NEW_URL;

    private static String AUTH_TOKEN = "";


    private RestClient() {
    }

    public static Retrofit getRetrofit() {
        return RETROFIT;
    }

    public static RetrofitInterface get() {
        setupRestClient();
        return REST_CLIENT;
    }



    public static void resetRestClient() {
        REST_CLIENT = null;
        AUTH_TOKEN = "";
    }

    private static void setupRestClient() {

        //https://fnid1f60cca1.us2.hana.ondemand.com/gagf/services/java/service.xsjs/checkin
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        if (TextUtils.isEmpty(AUTH_TOKEN)) {
            AUTH_TOKEN = "Basic aW90NGRlY2lzaW9uOkZpcnN0QDIwMTc=";//Basic eWFyZDp5YXJk
        }

        try {
            builder.sslSocketFactory(new TLSSocketFactory());
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();

        // Can be Level.BASIC, Level.HEADERS, or Level.BODY
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        builder.networkInterceptors().add(httpLoggingInterceptor);
        builder.addNetworkInterceptor(new StethoInterceptor());

        builder.readTimeout(30, TimeUnit.SECONDS);
        builder.connectTimeout(30, TimeUnit.SECONDS);

        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").create();

        RxJavaCallAdapterFactory rxAdapter = RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io());

        String url = TextUtils.isEmpty(NEW_URL) ? BASE_URL : NEW_URL;

        Retrofit retrofit = new Retrofit.Builder()
                .client(builder.build())
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(rxAdapter)
                .build();

        RETROFIT = retrofit;
        REST_CLIENT = retrofit.create(RetrofitInterface.class);
    }
}

