package gps.map.navigator.view.ui.fragment.listener;

import androidx.annotation.NonNull;

import java.util.List;

import javax.inject.Inject;

import gps.map.navigator.model.interfaces.IMapPlace;
import gps.map.navigator.model.interfaces.PlaceProxyListener;

public class DestinationChangeListener implements PlaceProxyListener {
    @Inject
    ISwipeRoute swipeRoute;

    @Inject
    DestinationChangeListener() {
    }

    @Override
    public void onPlaceLocated(@NonNull IMapPlace mapPlace) {
        swipeRoute.setOnlyDestination(mapPlace);
    }

    @Override
    public void onPlacesLocated(@NonNull List<IMapPlace> mapPlaces) {

    }
}