## Take-home task: Flight Ticket Booking API (Spring Boot + Java)

## Design and implement a small REST API for a flight ticket booking system. Share the github project once done. 
 ⏱ You’re not expected to spend more than 60 minutes.

Partial solutions are acceptable — focus on correctness and clarity. Detailed specs are missing on purpose. Focus on what you think are the most reasonable business requirements that can be met within 60 min of implementation. 
### Implementation requirements
- This exercise is expected to be performed using the help of AI agents. Use any AI system of your choice. 
Step 1 
- Only AI (an agent of your choice) must be used, no manual coding allowed except fixing compilation issues if necessary. Use an AI agent or system of your choice. You must provide each and every AI prompt you used to create & update the solution. Commit every iteration in the git repository. The AI prompt used for the iteration must be in the commit message. 
Step 2
- Improve the AI version of the solution using manual coding. Indicate in the commit message what are the areas of the generated code that you improved and why. What are the major issues with the generated solution that you might not have time to fix manually? Make a single commit with the final changes to the git repository. 

### Technical expectations
- Use Spring Boot and Java
- Single application instance (no distributed systems concerns)
- No authentication, authorization, rate limiting
- No flight search or destination logic
- All booking operations assume the client already knows the flight number, no search required
- In-memory storage only (no database required)
- Do not allow overbooking the flights
- Do not need APIs to retrieve bookings. Only to book. 
- Model REST endpoints as you see fit
- Use appropriate HTTP methods and status codes

### Deliverables
- Project is published on Github and the link to the project is provided
- Runnable Spring Boot project (Gradle or Maven)
- Short README.md including:
  - how to run the service
  - example requests
  - what you would improve if you had more time
