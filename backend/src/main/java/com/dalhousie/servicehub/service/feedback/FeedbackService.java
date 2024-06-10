package com.dalhousie.servicehub.service.feedback;

import com.dalhousie.servicehub.request.AddFeedbackRequest;
import com.dalhousie.servicehub.response.GetFeedbackResponse;
import com.dalhousie.servicehub.util.ResponseBody;

public interface FeedbackService {
    /**
     * Add feedback into the database
     * @param addFeedbackRequest Request body of feedback to add
     * @return Response body with data of type Object
     */
    ResponseBody<Object> addFeedback(AddFeedbackRequest addFeedbackRequest);

    /**
     * Get all feedbacks for the requesting user id
     * @param userId ID of the user to get all feedbacks
     * @return List of feedbacks of requesting user id
     */
    ResponseBody<GetFeedbackResponse> getFeedbacks(long userId);
}
