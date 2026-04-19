# ▣ Subspace Collaborative Project Management App

⧉  **Subspace** is a full-stack Android project management application built for team collaboration.
It allows teams to manage their work from a mobile app. The app supports offline usage by storing data locally and syncing with the server when the internet is available.

⧉ Projects | Tasks | Chat | Polls | Analytics | File Sharing | Team Management | Priority Tracking | Deadlines | Activity Logs | Notifications

## ⬤ Languages & Technologies
<br>
<table>
  <tr>
    <td align="center" width="96">
      <a href="#macropower-tech">
        <img width="500" height="700" alt="Screenshot_2026-02-18_204416-removebg-preview" src="https://github.com/user-attachments/assets/cf23ed12-9dde-4c7a-94e4-17c54cb52b45" />
      </a>
      <br>JWT
    </td>
        <td align="center" width="96"><img width="512" height="512" alt="IntelliJ IDEA" src="https://github.com/user-attachments/assets/56d811de-0168-4360-a4d4-dd9bb45cd052" />
      <a href="#macropower-tech">
      </a>
      <br>IntelliJ IDEA
    </td>
     <td align="center" width="96">
      <a href="#macropower-tech" >
        <img src="https://github.com/user-attachments/assets/ba747568-4fc7-434d-a934-689e44a2de6b" width="48" height="48" alt="kotlin" />
      </a>
      <br>Kotlin
    </td>
    <td align="center" width="96">
      <a href="#macropower-tech">
        <img src="https://github.com/user-attachments/assets/a4f5c3f6-baee-49a8-8f42-c7ba8a51ca90" width="48" height="48" alt="TypeScript" />
      </a>
      <br>Ktor
    </td>
    <td align="center" width="96">
      <a href="#macropower-tech" >
       <img width="320" height="180" alt="OIP-removebg-preview" src="https://github.com/user-attachments/assets/46eca784-ef8e-40b4-b194-6c30e896ec27" />
      </a>
      <br>Compose
    </td>
    <td align="center" width="96"> 
      <a href="#macropower-tech" >
        <img width="400" height="400" alt="1048085" src="https://github.com/user-attachments/assets/3acd83c4-1cc1-4967-a2a2-46573027baad" />
      </a>
      <br>AWS S3
    </td>
    <td align="center"  width="96">
      <a href="#macropower-tech">
       <img width="169" height="180" alt="OIP-removebg-preview (1)" src="https://github.com/user-attachments/assets/696b8707-0b80-4b28-96ab-c840841baa20" />
      </a>
      <br>AWS EC2
    </td>
      <td align="center" width="96">
      <a href="#macropower-tech" >
        <img width="180" height="180" alt="OIP-removebg-preview (2)" src="https://github.com/user-attachments/assets/c8cebe40-d479-4b11-bbd7-3f104fd8506d" />
      </a>
      <br>AWS RDS
    </td>
      <td align="center" width="96">
      <a href="#macropower-tech" >
        <img width="100" height="100" alt="image-removebg-preview" src="https://github.com/user-attachments/assets/fe8bfc48-49fb-4aae-bf8a-d8b03badf960" />
      </a>
      <br>WebSokets
      <td align="center" width="96">
      <a href="#macropower-tech" >
        <img src="https://github.com/user-attachments/assets/d9e9bcf4-fe28-41dc-a295-0ce0c58d9f1c" width="48" height="48" alt="Firebase" />
      </a>
      <br>FCM
    </td>
  </tr>
</table>

| JWT Authentication | Password Hashing | Protected API Routes |  S3 Signed URLs |
|--------------------|------------------|------------------|----------------------|

| MVVM Architecture | Repository Pattern | Retrofit |Kotlin Coroutines | Dependency Injection |
|-------------------|-------------------|-----------|------------------|-----------------------|


## ⬤ High Level Diagram

<p align="center">
  <img src="https://github.com/user-attachments/assets/b29c3224-3548-4c15-a8f6-fbd6476e9259" width="900" height="550" />
</p>

## ⬤ User Features
- 📝 Personalized Dashboard – View assigned tasks, project summaries, deadlines, and real-time progress in one place.
- 📌 Task Management – Create, update, delete, and track tasks with priority levels and status updates.
- 👥 Team Collaboration – Join projects, participate in group discussions, and stay synced with team activities.
- 💬 Integrated Project Chat – Communicate with team members through real-time chat with message history support and pagination.
- 📊 Project Analytics & Insights – Visualize project progress, task completion rates, and workload distribution.


<p>
  <img src="https://github.com/user-attachments/assets/af3495b5-0c80-4223-884e-eff28f2e6206" width="300"/>
  &nbsp;&nbsp;&nbsp;
  <img src="https://github.com/user-attachments/assets/425e478f-02a9-4e8b-b69e-77f2ee5c5c33" width="300"/>
</p>

## ⬤ Implemented Principles & System Design
- 🧱 MVVM Architecture – Clear separation between UI (View), business logic (ViewModel), and -   data layer for maintainable and testable code.
- 🧩 Modular & Layered Design – Separation of concerns across presentation, domain, and data layers.
- ☁️ Cloud-Native Infrastructure – AWS S3 for secure object storage and AWS RDS for managed relational database services.
- 🔐 Signed URL-based access control for secure file uploads and downloads.
- 🔄 Offline-First Strategy – Local caching with Room DB and background synchronization.
<!-- <img width="270" height="3000" alt="image" src="https://github.com/user-attachments/assets/e4e4ec0f-e2ef-4b33-8d3c-7d63deffba0e" />
<img width="270" height="2600" alt="image" src="https://github.com/user-attachments/assets/93fe23dc-2050-4627-80dd-9815544b51e2" />
<img width="270" height="2600" alt="image" src="https://github.com/user-attachments/assets/938eae7d-3ae1-4db4-aaf6-7418b4f7ba1c" />
 -->
<p>
  <img src="https://github.com/user-attachments/assets/e4e4ec0f-e2ef-4b33-8d3c-7d63deffba0e" width="280"/>
  &nbsp;
  <img src="https://github.com/user-attachments/assets/93fe23dc-2050-4627-80dd-9815544b51e2" width="280"/>
  &nbsp;
  <img src="https://github.com/user-attachments/assets/938eae7d-3ae1-4db4-aaf6-7418b4f7ba1c" width="280"/>
</p>


## ⬤ Real-Time Team Chat | Polls | Analytics
- 💬 Project-based group chat using WebSockets.
- 📄 Pagination support with lazy loading for efficient message retrieval.
- 🗳️ Project-based polls for collaborative decision-making.
- 📈 Real-time poll result tracking and participation insights.
- 📊 Advanced project analytics for tracking productivity and progress.

<img width="270" height="2000" alt="image" src="https://github.com/user-attachments/assets/03f9ed29-ed30-44a7-8c6b-597e9266d9a6" />
<img width="270" height="2000" alt="image" src="https://github.com/user-attachments/assets/f693c446-01e4-47d3-8d00-041eb1dd2909" />
<img width="270" height="2000" alt="image" src="https://github.com/user-attachments/assets/a5b0e64f-6e57-4bdb-9364-527175089764" />

## ⬤ Connect With Your Team!
- 🔍 User Search – Search and discover other users by name or profile details for seamless networking.
- 🤝 Connection Requests – Send, receive, and manage connection requests with accept/reject functionality.
- 🌐 User Connections Management – Build and maintain connections with real-time updates and secure user relationship handling.

<img width="270" height="2600" alt="image" src="https://github.com/user-attachments/assets/a37b9a8b-e75d-40d1-b9f6-866a120e5051" />
<img width="270" height="2600" alt="image" src="https://github.com/user-attachments/assets/cf2f6e44-63ed-490b-8ec0-f388b3f3cd44" />
<img width="270" height="2000" alt="image" src="https://github.com/user-attachments/assets/cbab6da6-0fd8-493e-822a-ce32fe06ed3f" />


## ⬤ Libraries Used

| Purpose | Library |
|--------|---------|
| UI | Jetpack Compose, Material3 |
| Networking | Retrofit |
| Dependency Injection | Hilt |
| Image Loading | Coil |
| Local Storage | DataStore |
| Charts | VICO CHARTS |
| Backend | Ktor |
| ORM | Exposed ORM |
| Authentication | JWT |


## ⬤ SQL EER Diagram

<img width="882" height="721" alt="SQL" src="https://github.com/user-attachments/assets/b9f0d56f-709a-475f-af3a-f744812bc6a2" />

## ⬤MVVM Architecture

```
app/src/main/java/com/example/linkit/
├── data/                          
│   ├── TokenStore.kt
│   ├── api/                       
│   │   ├── AnalyticsApiService.kt
│   │   ├── ApiService.kt
│   │   ├── ChatApi.kt
│   │   └── ProjectApiService.kt
│   │
│   ├── local/                     
│   │   ├── LinkitDatabase.kt
│   │   ├── dao/                    
│   │   │   ├── AttachmentDao.kt
│   │   │   ├── ChatMessageDao.kt
│   │   │   ├── ConnectionDao.kt
│   │   │   ├── ........
│   │   └── entities/           
│   │       ├── AttachmentEntity.kt
│   │       ├── ChatMessageEntity.kt
│   │       ├── ConnectionEntity.kt
│   │       ├── ........
│   │
│   ├── models/                    
│   │   ├── AnalyticsModels.kt
│   │   ├── ChatModels.kt
│   │   ├── PollModels.kt
│   │   ├── ProfileModels.kt
│   │   ├── ProjectModels.kt
│   │   └── ...........
│   └── repo/                   
│       ├── AnalyticsRepository.kt
│       ├── AuthRepository.kt
│       ├── ChatRepository.kt
│       ├── ChatWebSocketClient.kt
│       ├── ..........
│
├── di/
│   └── NetworkModule.kt   
├── network/
│   ├── AuthInterceptor.kt       
│   └── ResponseInterceptor.kt  
├── util/                         
│   ├── Constants.kt
│   ├── ImageUtils.kt
│   ├── JwtUtils.kt
│   ├── NetworkUtils.kt
│   ├── TimeUtils.kt
│   └── UiEvent.kt
│   ├── .........
│
├── view/                     
│   ├── components/                
│   ├── navigation/
│   │   └── NavGraph.kt         
│   └── screens/                 
│       ├── AnalyticsScreen.kt
│       ├── AuthScreen.kt
│       ├── ChatScreen.kt
│       ├── .......
│     
└── viewmodel/                   
    ├── AnalyticsViewModel.kt
    ├── AuthViewModel.kt
    ├── ChatViewModel.kt
    ├── ...........
```

---
