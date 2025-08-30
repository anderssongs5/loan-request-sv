package co.com.powerup.ags.loan.request.consumer.mapper;

import co.com.powerup.ags.loan.request.consumer.api.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface UserMapper {
    
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);
    
    co.com.powerup.ags.loan.request.model.user.User toDomain(User apiUser);
}