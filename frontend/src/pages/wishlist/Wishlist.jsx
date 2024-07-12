import React, { useState, useEffect } from 'react';
import { Container, FormControl, Dropdown, Spinner } from 'react-bootstrap';
import { HttpStatusCode } from "axios";
import { ENDPOINTS } from '../../utils/Constants';
import { useAxios } from '../../context/AxiosContext';
import AppToast from "../../components/app_toast/AppToast";
import ServiceCard from "../../components/service_card/ServiceCard";
import ServiceDetailsCard from "../../components/service_card/ServiceDetailsCard";
import "../dashboard/Dashboard.css";


export default function Wishlist() {
  const [showToast, setShowToast] = useState(false);
  const [toastMessage, setToastMessage] = useState("");
  const [toastTitle, setToastTitle] = useState("");
  const [feedbacks, setFeedbacks] = useState([]);
  const [providerInfo, setProviderInfo] = useState({});
  const [providerLoading, setProviderInfoLoading] = useState(false);
  const [selectedService, setSelectedService] = useState(null);
  const [services, setServices] = useState([]);
  const [loading, setLoading] = useState(false);

  const { getRequest, postRequest } = useAxios();

  useEffect(() => {
    fetchWishlistedServices();
  }, []);

  const removeFromWishlist = async (serviceId) => {
   console.log('clicked')
  };


  const fetchWishlistedServices = async () => {
    setLoading(true);
    try {
      const response = await getRequest(`${ENDPOINTS.GET_WISHLISTED_SERVICES}`, true);
      if (response.status === HttpStatusCode.Ok) {
        console.log(response)
        setServices(response.data.data);
      } else {
        setToastMessage('Error fetching services.');
        setToastTitle('Error');
        setShowToast(true);
      }
    } catch (error) {
      setToastMessage('Error fetching services.');
      setToastTitle('Error');
      setShowToast(true);
    }
    setLoading(false);
  };

  const handleServiceClick = async (service) => {
    setSelectedService(service);
    setProviderInfoLoading(true);
    try {
      const providerInforesponse = await getRequest(ENDPOINTS.PROVIDER_DETAILS, true, { providerId: service.providerId })
      const feedbackResponse = await getRequest(ENDPOINTS.GET_FEEDBACK, true, { userId: service.providerId });
      console.log(providerInforesponse);
      if (providerInforesponse.status === HttpStatusCode.Ok && feedbackResponse.status === HttpStatusCode.Ok) {
        setFeedbacks(feedbackResponse.data.data.feedbacks);
        setProviderInfo(providerInforesponse.data.data.provider);
      } else {
        setToastMessage('Error fetching feedbacks.');
        setToastTitle('Error');
        setShowToast(true);
      }
    } catch (error) {
      setToastMessage('Error fetching feedbacks.');
      setToastTitle('Error');
      setShowToast(true);
    }
    setProviderInfoLoading(false);
  };


  return (
    <Container fluid className="dashboard">
      <Container className='d-flex result-body'>
        <Container fluid className='child1 flex-column'>
          {loading ? (
            <Spinner animation="border" role="status">
              <span className="sr-only">Loading...</span>
            </Spinner>
          ) : (
            services.map((service, idx) => (
              <ServiceCard key={idx} service={service} onClick={() => handleServiceClick(service)} />
               
            ))
          )}
        </Container>

        <Container fluid className='child2'>
          {selectedService && (
            <ServiceDetailsCard
              selectedService={selectedService}
              providerLoading={providerLoading}
              feedbacks={feedbacks}
              providerInfo={providerInfo}
              rightIconOnclick={removeFromWishlist}
              currentPage="wishilist"
            />
          )}
        </Container>
      </Container>
      <AppToast show={showToast} setShow={setShowToast} title={toastTitle} message={toastMessage} />
    </Container>
  );
}
