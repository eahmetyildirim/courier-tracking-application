package com.migros.couriertracking.mapper;

import com.migros.couriertracking.dto.CourierLocationRequest;
import com.migros.couriertracking.model.CourierLocation;
import com.migros.couriertracking.model.Location;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = Location.class)
public interface CourierMapper {

    @Mapping(target = "courierId", source = "courierId")
    @Mapping(target = "location", expression = "java(new Location(request.lat(), request.lng()))")
    @Mapping(target = "time", source = "request.time")
    CourierLocation toLocation(Long courierId, CourierLocationRequest request);
}
