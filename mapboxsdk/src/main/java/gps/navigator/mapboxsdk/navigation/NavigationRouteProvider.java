package gps.navigator.mapboxsdk.navigation;

import android.content.Context;

import androidx.annotation.Nullable;

import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;

import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import gps.navigator.mapboxsdk.R;
import gps.navigator.mapboxsdk.interfaces.RouteReadyListener;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NavigationRouteProvider implements INavigationProvider {
    private Context context;
    private MapboxMap mapboxMap;
    private NavigationMapRoute navigationMapRoute;

    public NavigationRouteProvider(Context context, MapboxMap mapboxMap, MapView mapView, Style style) {
        this.context = context;
        this.mapboxMap = mapboxMap;
        initializeLocationComponent(mapboxMap, style);
        navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap);
    }

    @Override
    public void buildRoute(@Nullable final Point origin, @Nullable final Point destination, @Nullable final RouteReadyListener listener) {
        if (origin != null && destination != null) {
            getMarker(new LatLng(destination.latitude(), destination.longitude()));
        } else {
            return;
        }
        NavigationRoute.builder(context)
                .accessToken(context.getString(R.string.mapbox_access_token))
                .origin(origin)
                .destination(destination)
                .alternatives(true)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        DirectionsResponse directionsResponse = response.body();
                        if (directionsResponse == null) {
                            if (listener != null) {
                                listener.onBuildFailed(new Exception("No routes found, make sure you set the right user and access token."));
                            }
                        } else if (directionsResponse.routes().size() < 1) {
                            if (listener != null) {
                                listener.onBuildFailed(new Exception("No routes found"));
                            }
                        } else {
                            navigationMapRoute.addRoutes(directionsResponse.routes());
                            mapboxMap.easeCamera(CameraUpdateFactory.newLatLngBounds(
                                    new LatLngBounds.Builder()
                                            .include(getPlace(origin))
                                            .include(getPlace(destination))
                                            .build(), 150), 5000);
                        }
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                        if (listener != null) {
                            listener.onBuildFailed(new Exception(t));
                        }
                    }
                });
    }

    private Marker getMarker(LatLng position) {
        return mapboxMap.addMarker(new MarkerOptions().position(position));
    }

    @Override
    public void drawRoute(@Nullable final DirectionsRoute route) {
    }

    @SuppressWarnings("MissingPermission")
    private void initializeLocationComponent(MapboxMap mapboxMap, Style style) {
        LocationComponent component = mapboxMap.getLocationComponent();
        component.activateLocationComponent(context, style);

        component.setLocationComponentEnabled(true);
        component.setRenderMode(RenderMode.COMPASS);
        component.setCameraMode(CameraMode.TRACKING);
        component.zoomWhileTracking(10d);
    }

    private LatLng getPlace(Point origin) {
        return new LatLng(origin.latitude(), origin.longitude());
    }
}
