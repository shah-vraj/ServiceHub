import React from 'react';
import { Card } from 'react-bootstrap';
import default_profile_pic from "../../assets/default_profile_pic.png";
import { FaDollarSign } from 'react-icons/fa';
import './ServiceCard.css';

export default function ServiceCard({ service, onClick }) {

  return (
    <Card className="mb-3 service-card">
      <Card.Body>
        <div className="card-flex-container">
          <div className="d-flex">
            <a href="#" onClick={() => onClick(service)} className='me-3'>
              <Card.Img variant="top" src={default_profile_pic} className="service-image" />
            </a>
            <div className='d-flex flex-row justify-content-between align-items-center w-100'>

              <div className='d-flex flex-column'>

                <a href="#" onClick={() => onClick(service)} className="card-title-link">
                  {service.name}
                </a>

              </div>
            </div>
          </div>
          <div className="content-container">
            <Card.Text className="card-text-custom">
              {service.description}
            </Card.Text>
            <div className="details-container">
              <div className="detail-item">
                <FaDollarSign className="icon" />
                <b><span>{service.perHourRate}/hour</span></b>
              </div>
            </div>
          </div>
        </div>
      </Card.Body>
    </Card>
  );
}