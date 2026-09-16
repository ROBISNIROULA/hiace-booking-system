package com.hiace.hiacebooking.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EntityMapper {

	@Bean
	public ModelMapper modelMapper() {
		
		return new ModelMapper();
	}
	
}
