package com.dalhousie.servicehub.service.user_services;

import com.dalhousie.servicehub.config.AwsProperties;
import com.dalhousie.servicehub.dto.ServiceDto;
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
import com.dalhousie.servicehub.util.ResponseBody;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

import java.util.List;

import static com.dalhousie.servicehub.util.ResponseBody.ResultType.SUCCESS;

@RequiredArgsConstructor
public class ManageServiceImpl implements ManageService {

    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final ServiceMapper serviceMapper;
    private final SnsClient snsClient;
    private final AwsProperties awsProperties;

    @Override
    public ResponseBody<String> addService(AddServiceRequest addServiceRequest, Long providerId) {
        UserModel provider = userRepository.findById(providerId)
                .orElseThrow(() -> new UserNotFoundException("User not found for id: " + providerId));

        ServiceModel serviceModel = ServiceModel.builder()
                .description(addServiceRequest.getDescription())
                .name(addServiceRequest.getName())
                .perHourRate(addServiceRequest.getPerHourRate())
                .type(addServiceRequest.getType())
                .provider(provider)
                .build();
        serviceRepository.save(serviceModel);
        sendNotification(serviceModel);
        return new ResponseBody<>(SUCCESS, "", "Add service successful");
    }

    @Override
    public ResponseBody<GetServicesResponse> getUserServicesByProviderId(Long providerId) {
        UserModel user = userRepository.findById(providerId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + providerId));

        List<ServiceDto> services = serviceRepository.findByProviderId(user.getId())
                .stream()
                .map(serviceMapper::toDto)
                .toList();
        GetServicesResponse response = GetServicesResponse.builder()
                .services(services)
                .build();
        return new ResponseBody<>(SUCCESS, response, "Fetched user services successfully");
    }

    @Override
    public ResponseBody<String> deleteService(Long serviceId) {
        ServiceModel service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ServiceNotFoundException("Service not found with ID: " + serviceId));

        serviceRepository.delete(service);
        return new ResponseBody<>(SUCCESS, "", "Service deleted successfully");
    }

    @Override
    public ResponseBody<String> updateService(UpdateServiceRequest updateServiceRequest, Long providerId) {
        if (!serviceRepository.existsById(updateServiceRequest.getId()))
            throw new ServiceNotFoundException("Service not found for id: " + updateServiceRequest.getId());
        UserModel provider = userRepository.findById(providerId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + providerId));

        serviceRepository.updateService(
                updateServiceRequest.getId(),
                updateServiceRequest.getDescription(),
                updateServiceRequest.getName(),
                updateServiceRequest.getPerHourRate(),
                updateServiceRequest.getType(),
                provider
        );
        return new ResponseBody<>(SUCCESS, "", "Update service successful");
    }

    public void sendNotification(ServiceModel serviceModel) {
        String snsTopicArn = ensureTopicExists();
        String message = "New service added by " + serviceModel.getProvider().getUsername() + ": " + serviceModel.getName() + ". Check it out now!";
        PublishRequest publishRequest = PublishRequest.builder()
                .topicArn(snsTopicArn)
                .message(message)
                .subject("ServiceHub - " + serviceModel.getName())
                .build();

        snsClient.publish(publishRequest);
    }

    private String ensureTopicExists() {
        String topicArnPrefix = "arn:aws:sns:" + awsProperties.region + ":" + awsProperties.accountId + ":";
        String topicName = "ServiceHubAllUsers";
        ListTopicsResponse listTopicsResponse = snsClient.listTopics();
        for (Topic topic : listTopicsResponse.topics()) {
            if (topic.topicArn().equals(topicArnPrefix + topicName)) {
                return topic.topicArn();
            }
        }

        CreateTopicRequest createTopicRequest = CreateTopicRequest.builder()
                .name(topicName)
                .build();
        CreateTopicResponse createTopicResponse = snsClient.createTopic(createTopicRequest);
        return createTopicResponse.topicArn();
    }
}
