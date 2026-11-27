package com.swiftlicious.versionbridge.nms.entity.firework;

import com.swiftlicious.versionbridge.nms.entity.FakeEntity;

public interface FakeFirework extends FakeEntity {

	void flightTime(int flightTime);

	void invisible(boolean invisible);
}