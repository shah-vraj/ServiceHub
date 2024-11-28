package com.dalhousie.servicehub.service;

import com.dalhousie.servicehub.dto.ServiceDto;
import com.dalhousie.servicehub.enums.ServiceType;
import com.dalhousie.servicehub.exceptions.ServiceNotFoundException;
import com.dalhousie.servicehub.exceptions.UserNotFoundException;
import com.dalhousie.servicehub.mapper.ServiceMapper;
import com.dalhousie.servicehub.model.ServiceModel;
import com.dalhousie.servicehub.model.UserModel;
import com.dalhousie.servicehub.repository.ServiceRepository;
import com.dalhousie.servicehub.repository.UserRepository;
import com.dalhousie.servicehub.request.AddServiceRequest;
import com.dalhousie.servicehub.request.UpdateServiceRequest;
import com.dalhousie.servicehub.response.GetServicesResponse;
import com.dalhousie.servicehub.service.user_services.ManageServiceImpl;
import com.dalhousie.servicehub.util.ResponseBody;
import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static com.dalhousie.servicehub.util.ResponseBody.ResultType.SUCCESS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ManageServiceTest {

    private static final Logger logger = LogManager.getLogger(ManageServiceTest.class);

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ServiceMapper serviceMapper;

    @InjectMocks
    private ManageServiceImpl manageService;

    @Test
    public void shouldThrowUserNotFoundException_WhenUserNotRegistered_AndAddServiceIsCalled() {
        // Given
        long providerId = 10;
        AddServiceRequest addServiceRequest = AddServiceRequest.builder()
                .name("Test Service")
                .description("Test Description")
                .perHourRate(50.0)
                .type(ServiceType.Other)
                .build();
        logger.info("Starting test: Unregistered provider id provided to add service");

        // When
        when(userRepository.findById(providerId)).thenReturn(Optional.empty());
        logger.info("Inputting non-registered providerId: {}", addServiceRequest);

        // Then
        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> manageService.addService(addServiceRequest, providerId)
        );

        assertEquals(exception.getMessage(), "User not found for id: " + providerId);
        verify(serviceRepository, never()).save(any(ServiceModel.class));
        logger.info("Test completed: Unregistered provider id provided to add service");
    }

//    @Test
//    public void shouldAddService_WhenInputIsValid_AndAddServiceIsCalled() {
//        // Given
//        UserModel provider = UserModel.builder().id(10L).build();
//        AddServiceRequest addServiceRequest = AddServiceRequest.builder()
//                .name("Test Service")
//                .description("Test Description")
//                .perHourRate(50.0)
//                .type(ServiceType.Other)
//                .build();
//        logger.info("Starting test: Valid input provided to add service");
//
//        // When
//        when(userRepository.findById(provider.getId())).thenReturn(Optional.of(provider));
//        when(serviceRepository.save(any(ServiceModel.class))).thenReturn(new ServiceModel());
//        logger.info("Providing valid input to add service: {}", addServiceRequest);
//
//        ResponseBody<String> responseBody = manageService.addService(addServiceRequest, provider.getId());
//
//        // Then
//        logger.info("Response body after providing valid input: {}", responseBody);
//        verify(serviceRepository).save(any(ServiceModel.class));
//        assertEquals(responseBody.resultType(), SUCCESS);
//        assertEquals(responseBody.message(), "Add service successful");
//        logger.info("Test completed: Valid input provided to add service");
//    }

    @Test
    public void shouldThrowUserNotFoundException_WhenUserIsNotRegistered_AndGetUserServicesByProviderIdIsCalled() {
        // Given
        long providerId = 10;
        logger.info("Starting test: Unregistered provider id provided to get user services");

        // When
        when(userRepository.findById(providerId)).thenReturn(Optional.empty());
        logger.info("Passing non registered provider id to getUserServicesByProviderId: {}", providerId);

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> manageService.getUserServicesByProviderId(providerId)
        );

        // Then
        assertEquals(exception.getMessage(), "User not found with ID: " + providerId);
        verify(serviceRepository, never()).findByProviderId(anyLong());
        logger.info("Test completed: Unregistered provider id provided to get user services");
    }

    @Test
    public void shouldGetUserServicesByProviderId_WhenUserIsRegistered_AndServicesExist() {
        // Given
        long providerId = 10;
        UserModel user = new UserModel();
        user.setId(providerId);
        ServiceModel serviceModel = new ServiceModel();
        serviceModel.setId(1L);
        ServiceDto serviceDto = new ServiceDto();
        serviceDto.setId(1L);
        logger.info("Starting test: Registered provider id with existing services provided to get user services");

        // When
        when(userRepository.findById(providerId)).thenReturn(Optional.of(user));
        when(serviceRepository.findByProviderId(providerId)).thenReturn(Collections.singletonList(serviceModel));
        when(serviceMapper.toDto(any(ServiceModel.class))).thenReturn(serviceDto);
        logger.info("Passing registered provider id with existing services to getUserServicesByProviderId: {}", providerId);

        ResponseBody<GetServicesResponse> responseBody = manageService.getUserServicesByProviderId(providerId);

        // Then
        logger.info("Response body after getting user services: {}", responseBody);
        verify(serviceRepository).findByProviderId(providerId);
        assertEquals(responseBody.resultType(), SUCCESS);
        assertEquals(responseBody.message(), "Fetched user services successfully");
        assertNotNull(responseBody.data());
        assertEquals(responseBody.data().getServices().size(), 1);
        logger.info("Test completed: Registered provider id with existing services provided to get user services");
    }

    @Test
    public void shouldDeleteService_WhenServiceExists_AndDeleteServiceIsCalled() throws ServiceNotFoundException {
        // Given
        long serviceId = 1L;
        ServiceModel serviceModel = new ServiceModel();
        serviceModel.setId(serviceId);
        logger.info("Starting test: Existing service id provided to delete service");

        // When
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(serviceModel));
        logger.info("Providing existing service id to delete service: {}", serviceId);

        ResponseBody<String> responseBody = manageService.deleteService(serviceId);

        // Then
        logger.info("Response body after deleting service: {}", responseBody);
        verify(serviceRepository).delete(serviceModel);
        assertEquals(responseBody.resultType(), SUCCESS);
        assertEquals(responseBody.message(), "Service deleted successfully");
        logger.info("Test completed: Existing service id provided to delete service");
    }

    @Test
    public void shouldThrowServiceNotFoundException_WhenServiceDoesNotExist_AndDeleteServiceIsCalled() {
        // Given
        long serviceId = 1L;
        logger.info("Starting test: Non-existing service id provided to delete service");

        // When
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.empty());
        logger.info("Providing non-existing service id to delete service: {}", serviceId);

        ServiceNotFoundException exception = assertThrows(ServiceNotFoundException.class,
                () -> manageService.deleteService(serviceId)
        );

        // Then
        assertEquals(exception.getMessage(), "Service not found with ID: " + serviceId);
        verify(serviceRepository, never()).delete(any(ServiceModel.class));
        logger.info("Test completed: Non-existing service id provided to delete service");
    }

    @Test
    public void shouldUpdateService_WhenInputIsValid_AndUpdateServiceIsCalled() {
        // Given
        long serviceId = 1L;
        UserModel provider = UserModel.builder().id(10L).build();
        UpdateServiceRequest updateServiceRequest = UpdateServiceRequest.builder()
                .id(serviceId)
                .name("Updated Service")
                .description("Updated Description")
                .perHourRate(75.0)
                .type(ServiceType.HomeServices)
                .build();
        logger.info("Starting test: Valid input provided to update service");
        when(serviceRepository.existsById(serviceId)).thenReturn(true);

        // When user not found
        assertThrows(UserNotFoundException.class,
                () -> manageService.updateService(updateServiceRequest, provider.getId()));

        // When user found
        when(userRepository.findById(provider.getId())).thenReturn(Optional.of(provider));
        logger.info("Providing valid input to update service: {}", updateServiceRequest);

        ResponseBody<String> responseBody = manageService.updateService(updateServiceRequest, provider.getId());

        // Then
        logger.info("Response body after updating service: {}", responseBody);
        verify(serviceRepository).updateService(serviceId,
                updateServiceRequest.getDescription(),
                updateServiceRequest.getName(),
                updateServiceRequest.getPerHourRate(),
                updateServiceRequest.getType(),
                provider);
        assertEquals(ResponseBody.ResultType.SUCCESS, responseBody.resultType());
        assertEquals("Update service successful", responseBody.message());
        logger.info("Test completed: Valid input provided to update service");
    }

    @Test
    @Transactional
    public void shouldThrowServiceException_WhenServiceDoesNotExist_AndUpdateServiceIsCalled() {
        // Given
        long serviceId = 1L;
        long providerId = 10L;
        UpdateServiceRequest updateServiceRequest = UpdateServiceRequest.builder()
                .id(serviceId)
                .name("Updated Service")
                .description("Updated Description")
                .perHourRate(75.0)
                .type(ServiceType.HomeServices)
                .build();
        System.out.println("Starting test: Non-existing service id provided to update service");

        // Mock repository behavior
        when(serviceRepository.existsById(serviceId)).thenReturn(false);

        // When and Then
        ServiceNotFoundException exception = assertThrows(ServiceNotFoundException.class,
                () -> manageService.updateService(updateServiceRequest, providerId)
        );

        // Verify repository method was never called
        verify(serviceRepository, never()).updateService(anyLong(), anyString(), anyString(), anyDouble(), any(), any());

        // Assertions
        assertEquals("Service not found for id: " + serviceId, exception.getMessage());

        System.out.println("Test completed: Non-existing service id provided to update service");
    }
}
