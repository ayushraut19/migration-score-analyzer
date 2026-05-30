# Developer Guide

## 📚 Architecture Overview

The application follows **Model-View-Controller (MVC)** pattern with a service-oriented architecture.

```
┌─────────────────────────────────────────────────────┐
│                  UI (Swing Views)                   │
│  MainWindow → InputPanel | ResultsPanel | Compare   │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│            Controller (Business Logic)               │
│         RecommendationController                    │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│    Service Layer (Business Rules & API)             │
│ ┌──────────────┐  ┌──────────────┐  ┌────────────┐ │
│ │Recomm Service│  │ScoringEngine │  │DataService │ │
│ └──────────────┘  └──────────────┘  └────────────┘ │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│        Model (Data Structures)                       │
│ ┌────────┐  ┌──────────┐  ┌──────────────┐         │
│ │Locality│  │UserPref  │  │Recommendation│         │
│ └────────┘  └──────────┘  └──────────────┘         │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│           Utilities & Helpers                        │
│ ┌────────────┐  ┌──────────┐  ┌──────────────┐     │
│ │UIConstants │  │UIUtils   │  │JsonDataLoader│     │
│ └────────────┘  └──────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────┘
```

## 🔑 Core Components

### Models (`com.smartcity.model`)
- **Locality.java** - Represents a geographic locality
- **UserPreferences.java** - User's input and preferences
- **RecommendationResult.java** - Score result with breakdown

### Views (`com.smartcity.view`)
- **MainWindow.java** - Main application window with CardLayout
- **InputPanel.java** - User input and preference configuration
- **ResultsPanel.java** - Display top 3 recommendations
- **ComparisonPanel.java** - Side-by-side comparison table
- **ApplicationLauncher.java** - Application entry point

### Controller (`com.smartcity.controller`)
- **RecommendationController.java** - MVC controller with listener pattern
  - Manages user input
  - Triggers calculations
  - Updates views via listeners

### Services (`com.smartcity.service`)
- **RecommendationService.java** - Facade for recommendation operations
- **ScoringEngine.java** - Core scoring algorithm
- **DataService.java** - Data access and caching
- **FavoritesManager.java** - Manage user favorites

### Utils (`com.smartcity.utils`)
- **UIConstants.java** - Color, font, and dimension constants
- **UIUtils.java** - UI helper methods
- **ConfigLoader.java** - Configuration management
- **JsonDataLoader.java** - Load JSON data files
- **ValidationUtils.java** - Input validation helpers

## 🧮 Scoring Algorithm

### Formula
```
FinalScore = Σ (normalized_weight × normalized_metric) + adjustments
```

### Process
1. Get user weights (0-10)
2. Normalize weights: weight_i / Σ(all_weights)
3. Get locality metrics
4. Normalize metrics (0-10 scale)
5. Calculate weighted sum
6. Apply bonuses/penalties
7. Clamp to 0-10 range

### Example
```
Weights: Job=9, Cost=5, Safety=8 (total=22)
Normalized: Job=0.409, Cost=0.227, Safety=0.364

Metrics: Job=8, Cost=9, Safety=7
Weighted: (8×0.409) + (9×0.227) + (7×0.364) = 7.67
```

## 🔌 Adding New Features

### Add New Scoring Factor

1. **Update Model** (Locality.java)
```java
private double newFactor;
public double getNewFactor() { return newFactor; }
public void setNewFactor(double value) { this.newFactor = value; }
```

2. **Update Data** (localities.json)
```json
"newFactor": 7.5
```

3. **Update Scoring** (ScoringEngine.java)
```java
double newScore = Math.min(10, locality.getNewFactor());
breakdown.put("New Factor", newScore);
```

4. **Add UI Slider** (InputPanel.java)
```java
panel.add(createWeightSlider("New Factor", "newFactorWeight", 7));
```

5. **Update UserPreferences** (UserPreferences.java)
```java
private double newFactorWeight;
public double getNewFactorWeight() { return newFactorWeight; }
public void setNewFactorWeight(double weight) { 
    this.newFactorWeight = Math.max(0, Math.min(10, weight));
}
```

### Add New City

1. Add localities to `data/localities.json`
2. Restart application (auto-loads cities)

### Customize Scoring

Edit `ScoringEngine.applyAdjustments()` to add bonuses/penalties:
```java
if (someCondition) {
    adjustedScore += 0.5;
}
```

## 🧪 Testing

### Unit Testing
```java
public class ScoringEngineTest {
    @Test
    public void testScoreCalculation() {
        Locality locality = new Locality(...);
        UserPreferences prefs = new UserPreferences();
        ScoringEngine engine = new ScoringEngine();
        
        RecommendationResult result = engine.calculateScore(locality, prefs);
        assertTrue(result.getFinalScore() >= 0);
        assertTrue(result.getFinalScore() <= 10);
    }
}
```

### Manual Testing
1. Try different budget ranges
2. Test each profile type
3. Verify slider updates
4. Check score calculations

## 🎨 UI Customization

### Colors
Edit `UIConstants.java`:
```java
public static final Color PRIMARY_COLOR = new Color(0, 120, 215);
```

### Fonts
```java
public static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 24);
```

### Layout
Use GridBagLayout or BoxLayout in view components

### Example Custom Button
```java
JButton customBtn = UIUtils.createStyledButton("Click Me", Color.BLUE);
customBtn.setPreferredSize(new Dimension(150, 40));
```

## 🔐 Best Practices

### Code Organization
✅ DO:
- Separate concerns (M-V-C)
- Use service layer for business logic
- Create utility methods
- Add documentation comments
- Validate inputs

❌ DON'T:
- Mix UI and business logic
- Hardcode values
- Ignore null checks
- Create large monolithic classes

### Error Handling
```java
try {
    // operation
} catch (IOException e) {
    System.err.println("Error: " + e.getMessage());
    // Handle gracefully
}
```

### Performance
- Cache data (DataService does this)
- Use listeners instead of polling
- Compute only when needed
- Profile slow operations

## 📊 Data Format

### localities.json Structure
```json
{
  "id": "unique_id",
  "name": "Locality Name",
  "city": "City Name",
  "state": "State Name",
  "avgRent": 25000,           // Monthly rent
  "jobIndex": 8.5,             // 0-10
  "hospitalRating": 9.0,       // 0-10
  "transportScore": 7.5,       // 0-10
  "safetyScore": 8.0,          // 0-10
  "pollutionIndex": 6.0,       // 0-10 (higher = more pollution)
  "lifestyleScore": 8.5,       // 0-10
  "populationDensity": 6.5,    // 0-10
  "description": "Description text"
}
```

## 🚀 Deployment

### Build JAR
```bash
mvn clean package
```

### Run JAR
```bash
java -jar target/SmartCityRecommender.jar
```

### Create Executable
- Use `launch4j` for Windows .exe
- Use `appdmg` for macOS .dmg
- Create AppImage for Linux

## 🔗 Integration Points

### REST API Backend
Currently uses local data. To integrate REST API:

1. Create HTTP client in `RecommendationService`:
```java
private HttpClient client = HttpClient.newHttpClient();

public List<Locality> getLocalitiesFromAPI() {
    // Make HTTP GET request
    // Parse JSON response
    // Return localities
}
```

2. Replace DataService calls with API calls

### Database Integration
Add database layer:
```java
public class DatabaseService {
    private Connection connection;
    
    public List<Locality> getAllLocalities() {
        // Query database
    }
}
```

## 📈 Performance Optimization

### Current Performance
- Load time: < 1 second
- Scoring: < 50ms
- UI response: Immediate

### Future Improvements
- Cache scoring results
- Lazy load UI components
- Pagination for large result sets
- Index database queries

## 📝 Documentation Standards

### Comments
```java
/**
 * Brief description.
 * 
 * Detailed explanation if needed.
 * 
 * @param parameter Description
 * @return Description
 */
public void method(String parameter) {
    // Implementation
}
```

### Code Style
- Class names: PascalCase
- Variables: camelCase
- Constants: UPPER_CASE
- 4-space indentation
- Line length: 100 characters

## 🔄 Contributing

### Code Review Checklist
- [ ] Follows MVC pattern
- [ ] No hardcoded values
- [ ] Input validated
- [ ] Error handling present
- [ ] Comments added
- [ ] No null pointer issues
- [ ] Tested manually

---

**Happy Coding! 🚀**
