// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.webviewflutter;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import io.flutter.plugins.webviewflutter.GeneratedAndroidWebView.WebChromeClientHostApi;

/**
 * Host api implementation for {@link WebChromeClient}.
 *
 * <p>Handles creating {@link WebChromeClient}s that intercommunicate with a paired Dart object.
 */
public class WebChromeClientHostApiImpl implements WebChromeClientHostApi {
  private final InstanceManager instanceManager;
  private final WebChromeClientCreator webChromeClientCreator;
  private final WebChromeClientFlutterApiImpl flutterApi;

  private Context context;

  /**
   * Implementation of {@link WebChromeClient} that passes arguments of callback methods to Dart.
   */
  public static class WebChromeClientImpl extends WebChromeClient implements Releasable {
    @Nullable private WebChromeClientFlutterApiImpl flutterApi;
    private WebViewClient webViewClient;

    private Context mContext;

//    /** 视频全屏参数 */
//    protected static final FrameLayout.LayoutParams COVER_SCREEN_PARAMS = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//    private View customView;
//    private FrameLayout fullscreenContainer;
//    private WebChromeClient.CustomViewCallback customViewCallback;

      protected static final FrameLayout.LayoutParams FULLSCREEN_LAYOUT_PARAMS = new FrameLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER);

      @RequiresApi(api = Build.VERSION_CODES.KITKAT)
      protected static final int FULLSCREEN_SYSTEM_UI_VISIBILITY_KITKAT = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
              View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
              View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
              View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
              View.SYSTEM_UI_FLAG_FULLSCREEN |
              View.SYSTEM_UI_FLAG_IMMERSIVE |
              View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

      protected static final int FULLSCREEN_SYSTEM_UI_VISIBILITY = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
              View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
              View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
              View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
              View.SYSTEM_UI_FLAG_FULLSCREEN;
    boolean isFullScreen;
    View mCustomView;
    CustomViewCallback mCustomViewCallback;
    private int mOriginalOrientation;
    private int mOriginalSystemUiVisibility;

    /**
     * Creates a {@link WebChromeClient} that passes arguments of callbacks methods to Dart.
     *
     * @param flutterApi handles sending messages to Dart
     * @param webViewClient receives forwarded calls from {@link WebChromeClient#onCreateWindow}
     */
    public WebChromeClientImpl(
        @NonNull WebChromeClientFlutterApiImpl flutterApi, WebViewClient webViewClient, Context context) {
      this.flutterApi = flutterApi;
      this.webViewClient = webViewClient;
      this.mContext = context;
    }

    @Override
    public boolean onCreateWindow(
        final WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
      return onCreateWindow(view, resultMsg, new WebView(view.getContext()));
    }

    /**
     * Verifies that a url opened by `Window.open` has a secure url.
     *
     * @param view the WebView from which the request for a new window originated.
     * @param resultMsg the message to send when once a new WebView has been created. resultMsg.obj
     *     is a {@link WebView.WebViewTransport} object. This should be used to transport the new
     *     WebView, by calling WebView.WebViewTransport.setWebView(WebView)
     * @param onCreateWindowWebView the temporary WebView used to verify the url is secure
     * @return this method should return true if the host application will create a new window, in
     *     which case resultMsg should be sent to its target. Otherwise, this method should return
     *     false. Returning false from this method but also sending resultMsg will result in
     *     undefined behavior
     */
    @VisibleForTesting
    boolean onCreateWindow(
        final WebView view, Message resultMsg, @Nullable WebView onCreateWindowWebView) {
      final WebViewClient windowWebViewClient =
          new WebViewClient() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public boolean shouldOverrideUrlLoading(
                @NonNull WebView windowWebView, @NonNull WebResourceRequest request) {
              if (!webViewClient.shouldOverrideUrlLoading(view, request)) {
                view.loadUrl(request.getUrl().toString());
              }
              return true;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView windowWebView, String url) {
              if (!webViewClient.shouldOverrideUrlLoading(view, url)) {
                view.loadUrl(url);
              }
              return true;
            }
          };

      if (onCreateWindowWebView == null) {
        onCreateWindowWebView = new WebView(view.getContext());
      }
      onCreateWindowWebView.setWebViewClient(windowWebViewClient);

      final WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
      transport.setWebView(onCreateWindowWebView);
      resultMsg.sendToTarget();

      return true;
    }

    @Override
    public void onProgressChanged(WebView view, int progress) {
      if (flutterApi != null) {
        flutterApi.onProgressChanged(this, view, (long) progress, reply -> {});
      }
    }

      @Nullable
      protected ViewGroup getRootView() {
          Activity activity = (Activity) mContext;
          if (activity == null) {
              return null;
          }
          return (ViewGroup) activity.findViewById(android.R.id.content);
      }

    @Override
    public void onHideCustomView() {
      Activity activity = (Activity) mContext;
      if (activity == null) {
        return;
      }

      View decorView = getRootView();
      if (decorView == null) {
        return;
      }
      ((FrameLayout) decorView).removeView(this.mCustomView);
      this.mCustomView = null;
      decorView.setSystemUiVisibility(this.mOriginalSystemUiVisibility);
      activity.setRequestedOrientation(this.mOriginalOrientation);
      this.mCustomViewCallback.onCustomViewHidden();
      this.mCustomViewCallback = null;
      activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
      activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//      Map<String, Object> obj = new HashMap<>();
//      channel.invokeMethod("onExitFullscreen", obj);
    }


    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
        if (this.mCustomView != null) {
            onHideCustomView();
            return;
        }

        Activity activity = (Activity)mContext;
        if (activity == null) {
            return;
        }

        View decorView = getRootView();
        if (decorView == null) {
            return;
        }
        this.mCustomView = view;
        this.mOriginalSystemUiVisibility = decorView.getSystemUiVisibility();
        this.mOriginalOrientation = activity.getRequestedOrientation();
        this.mCustomViewCallback = callback;
        this.mCustomView.setBackgroundColor(Color.BLACK);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            decorView.setSystemUiVisibility(FULLSCREEN_SYSTEM_UI_VISIBILITY_KITKAT);
        } else {
            decorView.setSystemUiVisibility(FULLSCREEN_SYSTEM_UI_VISIBILITY);
        }
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        ((FrameLayout) decorView).addView(this.mCustomView, FULLSCREEN_LAYOUT_PARAMS);
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//        Map<String, Object> obj = new HashMap<>();
//        channel.invokeMethod("onEnterFullscreen", obj);
    }

      @Override
      public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback) {
          super.onShowCustomView(view, requestedOrientation, callback);
      }

      /**
     * Set the {@link WebViewClient} that calls to {@link WebChromeClient#onCreateWindow} are passed
     * to.
     *
     * @param webViewClient the forwarding {@link WebViewClient}
     */
    public void setWebViewClient(WebViewClient webViewClient) {
      this.webViewClient = webViewClient;
    }

      /**
       * Set the {@link WebViewClient} that calls to {@link WebChromeClient#onCreateWindow} are passed
       * to.
       *
       * @param webViewClient the forwarding {@link WebViewClient}
       */
      public void setWebViewClient(WebViewClient webViewClient, Context context) {
          this.webViewClient = webViewClient;
          this.mContext = context;
      }

    @Override
    public void release() {
      if (flutterApi != null) {
        flutterApi.dispose(this, reply -> {});
      }
      flutterApi = null;
    }
  }

  /** Handles creating {@link WebChromeClient}s for a {@link WebChromeClientHostApiImpl}. */
  public static class WebChromeClientCreator {
    /**
     * Creates a {@link DownloadListenerHostApiImpl.DownloadListenerImpl}.
     *
     * @param flutterApi handles sending messages to Dart
     * @param webViewClient receives forwarded calls from {@link WebChromeClient#onCreateWindow}
     * @return the created {@link DownloadListenerHostApiImpl.DownloadListenerImpl}
     */
    public WebChromeClientImpl createWebChromeClient(
        WebChromeClientFlutterApiImpl flutterApi, WebViewClient webViewClient, Context context) {
      return new WebChromeClientImpl(flutterApi, webViewClient, context);
    }
  }


    /**
     * Sets the context to construct {@link WebView}s.
     *
     * @param context the new context.
     */
    public void setContext(Context context) {
        this.context = context;
    }

  /**
   * Creates a host API that handles creating {@link WebChromeClient}s.
   *
   * @param instanceManager maintains instances stored to communicate with Dart objects
   * @param webChromeClientCreator handles creating {@link WebChromeClient}s
   * @param flutterApi handles sending messages to Dart
   */
  public WebChromeClientHostApiImpl(
      InstanceManager instanceManager,
      WebChromeClientCreator webChromeClientCreator,
      Context context,
      WebChromeClientFlutterApiImpl flutterApi) {
    this.instanceManager = instanceManager;
    this.webChromeClientCreator = webChromeClientCreator;
    this.context = context;
    this.flutterApi = flutterApi;
  }

  @Override
  public void create(Long instanceId, Long webViewClientInstanceId) {
    final WebViewClient webViewClient =
        (WebViewClient) instanceManager.getInstance(webViewClientInstanceId);
    final WebChromeClient webChromeClient =
        webChromeClientCreator.createWebChromeClient(flutterApi, webViewClient,context);
    instanceManager.addInstance(webChromeClient, instanceId);
  }
}
