#### Software Architecture and Platforms - a.y. 2025-2026

## Lab Activity #06-20251031  

v1.0.0-20251024

- **REST API**
  - Follow-up discussion after last lecture: an example of API not RESTful
  - Refining the TTT Game Server case study - `monolith_ttt_game_server` 
    - Same architecture/implementation of Lab Activity #05 for the domain and application layers
      - Modular monolith (hexagonal) + DDD domain pattern model
    - The refinement is about the controller, exposing a REST API
    	- [Open API spec](./doc/openapi.yaml)
  	- [Example of step-by-step interaction](./doc/session-with-monolith-ttt.md)

- From modular monolith to **microservices** distributed architecture: a first microservices based architecture for the TTT Game Server case study - `distributed_ttt`
  - Decomposing the monolith into three services:
    - `distributed_ttt.account_service`
      - managing accounts
    - `distributed_ttt.lobby_service`
      - managing user sessions
    - `distributed_ttt.game_service`
      - managing games
  - Each microservice has an hexagonal architecture, with its own domain model
  - Microservices collaboration
	- the `lobby_service` interacts with both the `account_service`, for validating users credentials when logging in, and the `game_service`, for creating and joining games
      - out-bound ports used in the business logic layer, implemented by proxy adapters in the infrastructure layer
  - [Example of step-by-step interaction](./doc/session-with-distributed-ttt.md)


 





  

