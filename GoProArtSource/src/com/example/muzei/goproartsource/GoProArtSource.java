/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.muzei.goproartsource;

import retrofit.ErrorHandler;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import android.content.Intent;
import android.net.Uri;

import com.example.muzei.goproartsource.GoProMediaService.MediaOfTheDay;
import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;

public class GoProArtSource extends RemoteMuzeiArtSource {
    private static final String TAG = "GoProArtSource";
    private static final String SOURCE_NAME = "GoProArtSource";

    private static final int ROTATE_TIME_MILLIS = 6 * 60 * 60 * 1000; // rotate every 3 hours

    public GoProArtSource() {
        super(SOURCE_NAME);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onTryUpdate(int reason) throws RetryException {
        RestAdapter restAdapter = new RestAdapter.Builder()
        .setServer("http://gopro.com")
        .setErrorHandler(new ErrorHandler() {
            @Override
            public Throwable handleError(RetrofitError retrofitError) {
                int statusCode = retrofitError.getResponse().getStatus();
                if (retrofitError.isNetworkError()
                        || (500 <= statusCode && statusCode < 600)) {
                    return new RetryException();
                }
                scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
                return retrofitError;
            }
        })
        .build();

        GoProMediaService service = restAdapter.create(GoProMediaService.class);
        MediaOfTheDay response = service.getMediaOfTheDay();

        if (response == null || response.pod == null) {
            throw new RetryException();
        }

        publishArtwork(new Artwork.Builder()
        .title(response.pod.title)
        .imageUri(Uri.parse(response.pod.l.image))
        .viewIntent(new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://gopro.com/photos/photo-of-the-day")))
                .build());

        scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
    }
}
