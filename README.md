# Smart City Recommendation System - Migration Score Analyzer

## 🎯 Overview
A production-level Java desktop application that recommends the best **LOCALITY** for users based on weighted factors such as job opportunities, cost of living, healthcare, transport, safety, environment, and lifestyle.

## 🏗️ Architecture
**Model-View-Controller (MVC) Architecture**

### Project Structure
```
migration-score-analyzer/
├── src/main/java/com/smartcity/
│   ├── model/                 # Data Models
│   │   ├── Locality.java
│   │   ├── UserPreferences.java
│   │   └── RecommendationResult.java
│   ├── view/                  # Swing UI Components
│   │   ├── MainWindow.java
│   │   ├── InputPanel.java
│   │   └── ResultsPanel.java
│   ├── controller/            # MVC Controller
│   │   └── RecommendationController.java
│   ├── service/               # Business Logic & API Layer
│   │   ├── RecommendationService.java
│   │   ├── ScoringEngine.java
│   │   ├── DataService.java
│   │   └── FavoritesManager.java
│   └── utils/                 # Utilities
│       ├── UIConstants.java
│       ├── UIUtils.java
│       ├── JsonDataLoader.java
│       └── ValidationUtils.java
├── data/
│   ├── localities.json        # Locality data
│   └── favorites.txt          # Saved favorites
├── pom.xml                    # Maven configuration
└── README.md                  # This file
```

## 🚀 Getting Started

### Prerequisites
- Java 11 or higher
- Maven 3.6+

### Build & Run

#### Option 1: Using Maven
```bash
# Build the project
mvn clean package

# Run the application
java -jar target/SmartCityRecommender.jar
```

#### Option 2: Using IDE
1. Open the project in your IDE (IntelliJ IDEA, Eclipse, VS Code)
2. Build the project
3. Run `com.smartcity.view.MainWindow` as Java Application

## 🧠 Core Features

### 1. User Input Panel
- **City Selection**: Choose from Bangalore, Mumbai, Delhi, Hyderabad
- **Budget Input**: Set annual budget (₹)
- **Family Size**: Specify household size
- **Work Type**: Select job type (On-site, Remote, Hybrid)
- **Preference Weights**: Adjust importance of each factor (0-10 scale)

### 2. Smart Recommendation Profiles
Auto-adjust weights based on profile:
- **Student**: High cost of living weight, lifestyle focus, low job weight
- **Bachelor**: High job opportunities, lifestyle, moderate cost awareness
- **Family**: Healthcare and safety priority, good cost balance

### 3. Scoring Engine
**Formula**: Score = Σ (normalized_weight × normalized_metric)

**Factors**:
- Job Opportunities (0-10)
- Cost of Living (0-10, inverse normalized by budget)
- Healthcare Quality (0-10)
- Transport Score (0-10)
- Safety Score (0-10)
- Environment Quality (0-10, inverse of pollution)
- Lifestyle Score (0-10)

**Adjustments**:
- Remote work bonus: +5%
- Family-specific bonuses for healthcare and safety

### 4. Results Dashboard
Displays top 3 recommended localities with:
- Final composite score
- Score breakdown with progress bars
- AI-generated explanation
- Google Maps integration
- Favorite locality saving

### 5. Real-Time Updates
- Sliders trigger instant recalculation
- Dynamic score updates
- Live preference preview

## 🎨 UI/UX Features

### Modern Styling
- Color-coded scores (Red < 6, Orange 6-8, Green ≥ 8)
- Rounded buttons with hover effects
- Clean card-based layout
- Professional typography (Segoe UI)
- Proper spacing and padding

### Dark Mode Ready
- Themeable color constants
- Can be extended for dark mode toggle

### Navigation
- Smooth transitions between input and results
- Back button for easy navigation
- Responsive layout

## 💾 Data Management

### JSON Data Storage
Locality data in `data/localities.json`:
```json
{
  "id": "BLR001",
  "name": "Whitefield",
  "city": "Bangalore",
  "state": "Karnataka",
  "avgRent": 25000,
  "jobIndex": 9.0,
  "hospitalRating": 8.0,
  "transportScore": 7.0,
  "safetyScore": 7.0,
  "pollutionIndex": 6.0,
  "lifestyleScore": 8.0,
  "populationDensity": 7.0,
  "description": "Tech hub with excellent job opportunities..."
}
```

### Favorites Management
User favorite localities stored in `data/favorites.txt`

## 🔧 Advanced Features

### API-Based Scoring
Service layer abstraction allows:
- Easy integration with REST APIs
- Scalable to microservices
- Currently uses local simulation
- Can extend to Node.js/Spring Boot backend

### Smart Recommendations
- Profile-based preset weights
- Budget-aware cost calculations
- Family-size considerations

### Extensibility
- Clean separation of concerns
- Easy to add new scoring factors
- Pluggable data sources

## 📊 Supported Cities & Localities

### Bangalore
- Whitefield, Koramangala, Indiranagar, Jayanagar, Electronic City

### Mumbai
- Bandra, Andheri, Dadar, Belapur

### New Delhi
- Gurgaon, Dwarka, Noida

### Hyderabad
- HITEC City, Jubilee Hills, Kukatpally

## 🔐 Code Quality

### OOP Principles
- Encapsulation: Private fields with getters/setters
- Inheritance: UI component hierarchy
- Polymorphism: Controller listener pattern
- Abstraction: Service layers

### Design Patterns
- **MVC Pattern**: Clear separation of concerns
- **Observer Pattern**: Controller listener mechanism
- **Singleton Pattern**: Service instances
- **Factory Pattern**: UI component creation

### Best Practices
- Comprehensive documentation
- Proper exception handling
- Input validation
- Resource management

## 🛠️ Customization

### Add New Localities
Edit `data/localities.json` and add entries

### Add New Scoring Factors
1. Update `Locality.java` model
2. Modify `ScoringEngine.calculateScore()`
3. Update UI sliders in `InputPanel.java`

### Add New Cities
1. Add locality data to JSON
2. Cities auto-load from data

### Styling Customization
Edit `UIConstants.java` for:
- Colors
- Fonts
- Dimensions
- Layouts

## 📈 Performance
- Fast JSON loading (cached)
- Instant scoring calculations
- Smooth UI responsiveness
- Minimal memory footprint

## 🚫 Known Limitations
- Currently uses local JSON data
- No database integration
- Favorites stored in plain text
- Single-user application

## 📝 Future Enhancements
- REST API backend integration
- Database (SQLite/PostgreSQL)
- User authentication
- Cloud data sync
- Multi-language support
- Advanced visualization charts
- Predictive analytics
- Mobile app

## 📄 Version
Version 1.0.0

## 👨‍💻 Author
Smart City Recommendation System Team

## 📞 Support
For issues or suggestions, please create a GitHub issue.

---

**Built with ❤️ using Java Swing MVC Architecture**
