# Joget Gemini AI Plugin

An intelligent AI-powered plugin for Joget that transforms your learning management system with advanced capabilities including automatic assignment grading, material evaluation, smart chat assistance, and comprehensive database integration.

![Version](https://img.shields.io/badge/version-2.4.0-blue.svg)
![Joget](https://img.shields.io/badge/joget-compatible-green.svg)
![AI](https://img.shields.io/badge/AI-Gemini%201.5-orange.svg)

## 🌟 Features

### 🤖 **Smart AI Chat Assistant**
- **Database-Enhanced Conversations**: Automatically detects course and assignment questions
- **Context-Aware Responses**: Uses real data from your Joget database
- **Multi-Language Support**: Powered by Google's Gemini 1.5 Flash model
- **Session Management**: Persistent chat history with database storage

### 🎯 **Automatic Assignment Grading**
- **Dual File Processing**: Analyzes both question files (from teachers) and answer files (from students)
- **Smart File Detection**: Automatically identifies file types based on naming conventions
- **Comprehensive Evaluation**: Grades based on correctness, completeness, and understanding
- **Detailed Feedback**: Provides specific remarks, strengths, and improvement suggestions
- **Batch Processing**: Grade multiple assignments simultaneously

### 📊 **Material Evaluation System**
- **Pre-Upload Analysis**: Evaluate materials before they're uploaded to students
- **Educational Quality Assessment**: 0-100% recommendation scores
- **Content Analysis**: Evaluates educational value, clarity, and completeness
- **Enhancement Suggestions**: Specific recommendations for improvement
- **Support for Multiple Formats**: PDF, DOCX, and TXT files

### 🗄️ **Database Integration**
- **Real-Time Data Access**: Direct integration with Joget's database
- **Smart Content Analysis**: Understands user intent for materials vs assignments
- **Advanced Search**: Keyword-based search across courses and assignments
- **Statistics Dashboard**: Comprehensive analytics on courses, materials, and assignments

### 🌐 **RESTful API Endpoints**
- **Chat API**: `/chat` - Intelligent conversation with database enhancement
- **Grading APIs**: `/grade` & `/grade/batch` - Single and batch assignment grading
- **Evaluation APIs**: `/evaluate` & `/evaluate/batch` - Material quality assessment
- **Database APIs**: Various endpoints for accessing course data
- **Admin APIs**: Health checks, debugging, and system monitoring

## 📋 Prerequisites

### Required Dependencies
Add these JAR files to your Joget classpath:

```
📦 Apache POI (for DOCX support)
├── poi-4.1.2.jar
└── poi-ooxml-4.1.2.jar

📦 Apache PDFBox (for PDF support)
└── pdfbox-2.0.24.jar

📦 Gson (for JSON processing)
└── gson-2.8.5.jar

📦 Apache HttpClient (for API calls)
├── httpclient-4.5.13.jar
└── httpcore-4.4.13.jar
```

### Database Requirements
- **MySQL/MariaDB**: Compatible with Joget's default database
- **Required Tables**: `app_fd_materials`, `app_fd_assignments`
- **Auto-Created Tables**: `gemini_chat_history` (created automatically)

### API Requirements
- **Google Gemini API Key**: Required for AI functionality
- **Internet Connection**: For API calls to Google's Gemini service

## 🚀 Installation

### 1. Build the Plugin
```bash
# Clone the repository
git clone <repository-url>
cd joget-gemini-plugin

# Build the OSGi bundle
mvn clean package

# Or build manually if using IDE
# Ensure all dependencies are in classpath
```

### 2. Deploy to Joget
```bash
# Copy the built JAR to Joget's plugin directory
cp target/gemini-plugin-2.4.0.jar /path/to/joget/wflow/plugins/

# Or upload via Joget Admin Console
# Go to: Settings > Manage Plugins > Upload Plugin
```

### 3. Configure API Key
Edit `GeminiPlugin.java` or set system properties:

```java
// Option 1: Edit the getConfiguredApiKey() method
private static String getConfiguredApiKey() {
    return "YOUR_ACTUAL_GEMINI_API_KEY_HERE";
}

// Option 2: Set system property
-Dgemini.api.key=YOUR_ACTUAL_GEMINI_API_KEY_HERE

// Option 3: Set environment variable
export GEMINI_API_KEY=YOUR_ACTUAL_GEMINI_API_KEY_HERE
```

### 4. File Upload Configuration
Ensure the upload path exists:
```bash
mkdir -p /path/to/joget/wflow/app_formuploads/assignments/
mkdir -p /path/to/joget/wflow/app_formuploads/materials/
```

## ⚡ Quick Start

### Start the Plugin
The plugin automatically starts when Joget loads. Check the logs for:
```
=== Gemini Plugin Bundle Started ===
🚀 EMBEDDED HTTP SERVER: Port 8081 - RUNNING ✅
🗄️ DATABASE ENDPOINTS: Connection - CONNECTED ✅
```

### Test the Installation
```bash
# Health check
curl "http://localhost:8081/health"

# Test chat
curl -X POST "http://localhost:8081/chat" \
  -d "userPrompt=Hello, are you working?&sessionId=test123"

# Test database connection
curl "http://localhost:8081/db/test"
```

### Access Interactive Documentation
Visit: `http://localhost:8081/` for the full API documentation with test buttons.

## 📖 API Reference

### 🤖 Chat API
```http
POST /chat
Content-Type: application/x-www-form-urlencoded

userPrompt=what courses do we have?
sessionId=user123
saveToDb=true
chatHistory=[previous conversation]
```

**Smart Features:**
- Automatically detects course/assignment questions
- Enhances responses with database data
- Supports natural language queries

### 🎯 Auto-Grading API

#### Single Assignment Grading
```http
POST /grade
Content-Type: application/x-www-form-urlencoded

assignmentId=12345
mode=preview  # or "save" to update database
```

#### Batch Grading
```http
GET /grade/batch?course=Programming101&status=ungraded&limit=10
```

### 📊 Material Evaluation API

#### Single Material Evaluation
```http
POST /evaluate
Content-Type: application/json

{
  "course": "Programming101",
  "description": "Java programming tutorial",
  "filename": "java_basics.pdf",
  "fileContent": "...",  # Optional: direct file content
  "preUpload": "true"    # Optional: pre-upload analysis
}
```

#### Batch Evaluation
```http
GET /evaluate/batch?course=Programming101&limit=5
```

### 🗄️ Database APIs

#### Course Materials
```http
GET /db/materials                    # Get all materials
GET /db/materials?search=java        # Search materials
GET /db/materials?course=Programming101  # Filter by course
```

#### Assignments
```http
GET /db/assignments                  # Get all assignments
GET /db/assignments?search=homework  # Search assignments
GET /db/assignments?status=completed # Filter by status
GET /db/assignments?upcoming=true    # Get upcoming assignments
```

#### Statistics
```http
GET /db/statistics  # Get comprehensive course statistics
```

## 🔧 Configuration

### Database Configuration
The plugin automatically detects Joget's database configuration. For manual configuration, edit `DatabaseService.java`:

```java
private static final String DB_HOST = "localhost";
private static final String DB_PORT = "3307";
private static final String DB_NAME = "jwdb";
private static final String DB_USER = "root";
private static final String DB_PASSWORD = "";
```

### File Upload Paths
Configure upload paths in the service classes:
```java
private static final String UPLOAD_PATH = "wflow/app_formuploads/";
```

### AI Model Parameters
Adjust AI behavior in the service classes:
```java
Map<String, Object> params = new HashMap<>();
params.put("temperature", 0.3);        // Lower = more consistent
params.put("maxOutputTokens", 1500);   // Response length limit
```

## 📁 File Structure

```
joget-gemini-plugin/
├── src/main/java/org/joget/gemini/
│   ├── GeminiPlugin.java           # Main plugin class with HTTP server
│   ├── GeminiService.java          # Gemini API integration
│   ├── AutoGradingService.java     # Assignment grading logic
│   ├── MaterialEvaluationService.java  # Material evaluation logic
│   ├── DatabaseService.java       # Database operations
│   ├── ContentAnalyzer.java       # Message intent analysis
│   └── Activator.java             # OSGi bundle activator
├── README.md                       # This file
└── pom.xml                         # Maven configuration
```

## 🔍 Usage Examples

### Intelligent Chat Scenarios

```bash
# Course Information
curl -X POST "http://localhost:8081/chat" \
  -d "userPrompt=what courses do we actually have?&sessionId=student123"

# Assignment Queries
curl -X POST "http://localhost:8081/chat" \
  -d "userPrompt=show me all assignments due this week&sessionId=student123"

# Material Search
curl -X POST "http://localhost:8081/chat" \
  -d "userPrompt=find materials about networking&sessionId=student123"
```

### Automated Grading Workflow

```bash
# 1. Get assignment IDs
curl "http://localhost:8081/db/assignments"

# 2. Preview grade for assignment
curl -X POST "http://localhost:8081/grade" \
  -d "assignmentId=12345&mode=preview"

# 3. Apply grade if satisfied
curl -X POST "http://localhost:8081/grade" \
  -d "assignmentId=12345&mode=save"

# 4. Batch grade all ungraded assignments
curl "http://localhost:8081/grade/batch?status=ungraded&limit=10"
```

### Material Quality Assessment

```bash
# Evaluate before upload (pre-upload analysis)
curl -X POST "http://localhost:8081/evaluate" \
  -H "Content-Type: application/json" \
  -d '{
    "course": "Programming101",
    "description": "Introduction to Java programming",
    "filename": "java_intro.pdf",
    "preUpload": "true"
  }'

# Batch evaluate existing materials
curl "http://localhost:8081/evaluate/batch?course=Programming101&limit=5"
```

## 🎨 Database Schema

### Expected Table Structures

#### app_fd_materials
```sql
CREATE TABLE app_fd_materials (
  id VARCHAR(255) PRIMARY KEY,
  dateCreated DATETIME,
  dateModified DATETIME,
  createdBy VARCHAR(255),
  createdByName VARCHAR(255),
  c_select_course VARCHAR(255),      # Course name
  c_course_fileupload VARCHAR(255),  # File name
  c_course_information TEXT,         # Description
  c_Uploaded_data VARCHAR(255)       # Upload date
);
```

#### app_fd_assignments
```sql
CREATE TABLE app_fd_assignments (
  id VARCHAR(255) PRIMARY KEY,
  dateCreated DATETIME,
  dateModified DATETIME,
  createdBy VARCHAR(255),
  createdByName VARCHAR(255),
  c_assignment_title VARCHAR(255),        # Assignment title
  c_course VARCHAR(255),                  # Course name
  c_due_date VARCHAR(255),               # Due date
  c_student_name VARCHAR(255),           # Student name
  c_field3 TEXT,                         # Student answer
  c_answer TEXT,                         # Alternative answer field
  c_field2 VARCHAR(255),                 # Uploaded file name
  c_assignment_completion VARCHAR(50),    # Completion status
  c_assignment_grade VARCHAR(50),        # Grade
  c_assignment_remarks_teacher TEXT      # Teacher remarks
);
```

## 🐛 Troubleshooting

### Common Issues

#### 1. Plugin Won't Start
```bash
# Check Joget logs for errors
tail -f /path/to/joget/logs/joget.log

# Common causes:
# - Missing dependencies (POI, PDFBox)
# - Incorrect API key
# - Database connection issues
```

#### 2. API Key Issues
```bash
# Test API key manually
curl -X POST "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent?key=YOUR_KEY" \
  -H "Content-Type: application/json" \
  -d '{"contents":[{"parts":[{"text":"test"}]}]}'
```

#### 3. Database Connection Problems
```bash
# Test database connection
curl "http://localhost:8081/db/test"

# Check database credentials in DatabaseService.java
# Verify Joget's database configuration
```

#### 4. File Upload Issues
```bash
# Check file permissions
ls -la wflow/app_formuploads/

# Create missing directories
mkdir -p wflow/app_formuploads/assignments/
mkdir -p wflow/app_formuploads/materials/
```

#### 5. Grading Not Working
- Verify assignment IDs exist in database
- Check file upload paths are correct
- Ensure POI and PDFBox dependencies are available
- Verify file formats are supported (PDF, DOCX, TXT)

### Debug Mode
Enable detailed logging by accessing:
```
http://localhost:8081/debug
```

## 🔐 Security Considerations

### API Key Protection
- Never commit API keys to version control
- Use environment variables or secure configuration
- Regularly rotate API keys
- Monitor API usage and quotas

### Database Security
- Use read-only database users when possible
- Implement proper input validation
- Enable SQL injection protection
- Regular security audits

### File Upload Security
- Validate file types and sizes
- Scan uploaded files for malware
- Implement access controls
- Regular cleanup of temporary files

## 📊 Performance Tips

### Optimization Strategies
1. **Database Connection Pooling**: Configure appropriate pool sizes
2. **API Rate Limiting**: Implement delays between batch operations
3. **File Caching**: Cache frequently accessed file content
4. **Response Compression**: Enable gzip compression for large responses
5. **Async Processing**: Use background jobs for large batch operations

### Monitoring
- Monitor API usage quotas
- Track response times
- Monitor database performance
- Log error rates and types

## 🤝 Contributing

### Development Setup
```bash
# Clone repository
git clone <repository-url>
cd joget-gemini-plugin

# Set up development environment
export GEMINI_API_KEY=your_test_key
export JOGET_HOME=/path/to/joget

# Run tests
mvn test

# Build plugin
mvn clean package
```

### Code Style
- Follow Java naming conventions
- Add comprehensive JavaDoc comments
- Include unit tests for new features
- Use proper error handling and logging

### Submitting Changes
1. Fork the repository
2. Create a feature branch
3. Make your changes with tests
4. Submit a pull request with clear description

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- **Joget Team**: For the excellent workflow platform
- **Google AI**: For the powerful Gemini API
- **Apache Foundation**: For POI and PDFBox libraries
- **Community Contributors**: For feedback and improvements

## 📞 Support

### Getting Help
- 📚 **Documentation**: Check this README and inline code comments
- 🐛 **Issues**: Report bugs via GitHub issues
- 💬 **Discussions**: Join community discussions
- 📧 **Contact**: Reach out to maintainers

### Useful Links
- [Joget Documentation](https://dev.joget.org/)
- [Google Gemini API Docs](https://developers.generativeai.google/)
- [Apache POI Documentation](https://poi.apache.org/)
- [Apache PDFBox Documentation](https://pdfbox.apache.org/)

---

**Made with ❤️ for the Joget Community**

*Transform your learning management system with AI-powered intelligence!*