# Fabflix Deployment

This project contains the files to deploy the Fabflix movie web application using Docker and Kubernetes.  
Fabflix is a movie website where users can search for films stored in a database.  
These files handle how the app is packaged, deployed, and made available online.

## What's Inside
- **Dockerfile** – builds a Docker image for the Fabflix application  
- **fabflix.yaml** – Kubernetes Deployment and Service to run Fabflix in a cluster  
- **ingress-multi.yaml** – Kubernetes Ingress for routing external traffic to the app  

## How It Works
1. The Dockerfile packages the Fabflix app into a container  
2. The fabflix.yaml file deploys that container to a Kubernetes cluster  
3. The ingress-multi.yaml file makes the app accessible from outside the cluster  

### Team
- cs122b-winter25-aj  

### Names
- Alfredo Leon  
- Juan Varela  

### Project 5 Video Demo
- https://www.youtube.com/watch?v=unm90Lqf3P8

### Endpoints
**Common**
- `doFilter /*`

**Login**
- `POST /api/employeeLogin`  
- `POST /api/login`

**Movies**
- `POST /api/add_movie`  
- `POST /api/add_star`  
- `POST /api/autocomplete`  
- `POST /api/cart`  
- `GET /api/checkout`  
- `GET /api/confirmation`  
- `GET /api/main_page`  
- `GET /api/movie`  
- `POST /api/process-payment`  
- `GET /api/results`  
- `GET /api/star`  
- `GET /api/top20`  

