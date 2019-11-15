package gps.map.navigator.view.ui.fragment.listener;

import androidx.annotation.NonNull;

import java.util.List;

import javax.inject.Inject;

import gps.map.navigator.model.interfaces.IMapPlace;
import gps.map.navigator.model.interfaces.PlaceProxyListener;

public class OriginChangeListener implements PlaceProxyListener {
    @Inject
    ISwipeRoute swipeRoute;

    @Inject
    OriginChangeListener() {
    }

    @Override
    public void onPlaceLocated(@NonNull IMapPlace mapPlace) {
        swipeRoute.setOnlyOrigin(mapPlace);
    }

    @Override
    public void onPlacesLocated(@NonNull List<IMapPlace> mapPlaces) {

    }
}