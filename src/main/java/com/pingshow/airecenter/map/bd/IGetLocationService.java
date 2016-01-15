package com.pingshow.airecenter.map.bd;

import com.baidu.platform.comapi.basestruct.GeoPoint;

public interface IGetLocationService {
	void startLocation();
	GeoPoint getMyGeo();
	void refreshLocation();
	void stopListener();
}
