# Exam Invigilator Assignment System

> An intelligent distributed system for automated exam invigilator (proctor) assignment using server-client architecture with advanced scheduling algorithms.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [Installation](#installation)
- [Usage](#usage)
- [Project Structure](#project-structure)
- [How It Works](#how-it-works)
- [Contributing](#contributing)
- [License](#license)

---

## 🎯 Overview

This system automates the complex task of assigning invigilators and proctors to exam rooms. It processes a spreadsheet of staff members and intelligently distributes them across multiple exam rooms and shifts using a sophisticated assignment algorithm.

**Key Highlights:**
- ✅ Automated assignment balancing
- ✅ Real-time processing with GUI
- ✅ Network-based client-server architecture
- ✅ Excel import/export functionality
- ✅ Conflict avoidance & history tracking

---

## ✨ Features

### Core Functionality
- **Intelligent Assignment Algorithm**
  - Automatically assigns proctors to exam rooms
  - Balances workload across multiple shifts (sessions)
  - Avoids assigning the same pair to the same room twice
  - Prevents repeat partnerships between staff members

- **Dual Role Support**
  - **Exam Room Proctors**: 2 per room (Main Proctor & Assistant Proctor)
  - **Corridor Supervisors**: Oversee exam hall corridors and conduct surveillance

- **Data Management**
  - Import staff lists from Excel (`.xlsx` format)
  - Automatic data validation
  - Export assignment results to formatted Excel reports
  - Separate reports for proctors and supervisors

### User Interface
- Modern dark-themed GUI (built with Java Swing)
- Real-time logging and status monitoring
- Responsive table views with color-coded results
- Connection statistics and metrics

### Network Features
- TCP socket-based communication
- Server listens on port 8888
- Support for multiple concurrent client connections
- Automatic IP detection for network configuration

---

## 🛠️ Technology Stack

| Technology | Purpose |
|-----------|---------|
| **Java** | Core language for application development |
| **Swing** | GUI framework for desktop interface |
| **Apache POI** | Excel file reading/writing (.xlsx format) |
| **Socket API** | Network communication (TCP/IP) |
| **Maven** | Build automation (optional) |

**Java Version:** 11 or higher recommended

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                   CLIENT APPLICATION                     │
│  • Excel file selection                                  │
│  • Parameter configuration (rooms, sessions, staff)      │
│  • Results display & export                              │
│  • Network communication to server                       │
└──────────────────────┬──────────────────────────────────┘
                       │
                    TCP/IP
                  (Port 8888)
                       │
┌──────────────────────▼──────────────────────────────────┐
│                   SERVER APPLICATION                     │
│  • Request listening & processing                        │
│  • Excel parsing & data extraction                       │
│  • Assignment algorithm execution                        │
│  • Results serialization                                 │
│  • Connection logging & statistics                       │
└─────────────────────────────────────────────────────────┘

         Shared Communication Models
         ├── CanBo (Staff member)
         ├── PhongThi (Exam room)
         ├── KetQuaPhanCong (Assignment result)
         └── YeuCau (Request)
```

---

## 📦 Installation

### Prerequisites
- **Java Development Kit (JDK)** 11+
- **Maven** (optional, for building)
- Network connectivity between client and server machines

### Step 1: Clone the Repository
```bash
git clone https://github.com/DaoQuan03/exam-invigilator-assignment-system.git
cd exam-invigilator-assignment-system
```

### Step 2: Compile the Project

**Using Maven:**
```bash
mvn clean compile
mvn package
```

**Using Java Compiler (Manual):**
```bash
# Compile all Java files
javac -cp ".;libs/*" *.java

# Or use the provided batch files (Windows)
```

### Step 3: Required Dependencies

Add Apache POI to your project. If using Maven, add to `pom.xml`:
```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.3</version>
</dependency>
```

For manual setup, download POI JAR files and place in the `libs/` directory.

---

## 🚀 Usage

### Starting the Server

**Windows:**
```bash
run_server.bat
```

**Linux/Mac:**
```bash
java -cp ".;libs/*" ServerApp
```

Expected output:
```
[HH:mm:ss] ✔ Server started at PORT 8888
[HH:mm:ss] ✔ Server LAN IP: 192.168.x.x
[HH:mm:ss] ✔ Clients connect to: 192.168.x.x:8888
```

### Starting the Client

**Windows:**
```bash
run_client.bat
```

**Linux/Mac:**
```bash
java -cp ".;libs/*" ClientApp
```

### Using the Client Application

1. **Enter Server IP**
   - Input the server's IP address (displayed on server console)
   - Default: `localhost` for local testing

2. **Select Staff Excel File**
   - Click "📂 Choose File"
   - Select an `.xlsx` file with staff data
   - Expected columns: `Mã GV` (Staff Code), `Họ và tên` (Full Name)

3. **Configure Parameters**
   - **Total Staff**: Number of staff members to use
   - **Exam Rooms**: Number of exam rooms
   - **Sessions**: Number of exam sessions/shifts

4. **Send Request to Server**
   - Click "🚀 Send to Server"
   - Wait for processing (status shown in log area)

5. **View & Export Results**
   - Results display in two tables:
     - 🎓 Exam Room Proctors
     - 🛡️ Corridor Supervisors
   - Click "💾 Export Excel" to save formatted reports

---

## 📂 Project Structure

```
exam-invigilator-assignment-system/
├── README.md                 # This file
├── pom.xml                   # Maven configuration
├── ClientApp.java            # Client GUI & application logic
├── ServerApp.java            # Server listener & processing engine
├── SharedModels.java         # Data classes for communication
├── run_client.bat            # Client startup script (Windows)
├── run_server.bat            # Server startup script (Windows)
├── libs/                     # External dependencies (POI, etc.)
├── bin/                      # Compiled .class files
└── .vscode/                  # VS Code configuration
```

### Key Classes

**ClientApp.java** (≈550 lines)
- Main GUI window with dark theme
- File selection dialog
- Server communication logic
- Excel export functionality
- Real-time results display

**ServerApp.java** (≈534 lines)
- Server socket listener
- Client connection handler
- Excel parsing & data extraction
- Smart assignment algorithm
- Statistics & logging

**SharedModels.java** (≈75 lines)
- `CanBo` - Staff member data class
- `PhongThi` - Exam room definition
- `KetQuaPhanCong` - Assignment result
- `YeuCau` - Client request object

---

## 🧠 How It Works

### Assignment Algorithm

The core algorithm balances staff assignments across exam sessions with intelligent conflict avoidance:

```
1. Load staff data from Excel file
2. Split staff into two groups:
   └─ Group A: Exam room proctors (2 × number of rooms)
   └─ Group B: Corridor supervisors (remaining staff)

3. For each exam session:
   a) Shuffle Group A for randomization
   b) For each exam room:
      • Find two available proctors
      • Ensure they haven't worked same room before
      • Ensure they haven't been paired before
      • Assign as Main & Assistant Proctor
   c) Assign all Group B staff as corridor supervisors

4. Export results to formatted Excel reports
```

### Key Features of the Algorithm

| Feature | Benefit |
|---------|---------|
| **Conflict Tracking** | Maintains history of room assignments and partnerships |
| **Smart Pairing** | Avoids repeating the same pairs in different sessions |
| **Fair Distribution** | Ensures balanced workload across all staff |
| **Randomization** | Prevents predictable assignment patterns |

---

## 📊 Input File Format

Your Excel file should have the following structure:

| Column | Header | Example |
|--------|--------|---------|
| A | Mã GV (Staff Code) | GV001 |
| B | Họ và tên (Full Name) | Nguyễn Văn A |

**Example Input:**
```
Mã GV    | Họ và tên
---------|------------------
GV001    | Nguyễn Văn A
GV002    | Trần Thị B
GV003    | Lê Văn C
...      | ...
```

---

## 📊 Output Format

### Exported Reports

**1. CoiThi_PhanCong.xlsx** (Exam Room Proctors)
```
Session | Room | Staff Code | Name | Role
--------|------|------------|------|----------
1       | 001  | GV001      | Mr.A | Main
1       | 001  | GV002      | Ms.B | Assistant
1       | 002  | GV003      | Mr.C | Main
...     | ...  | ...        | ...  | ...
```

**2. GiamSat_PhanCong.xlsx** (Corridor Supervisors)
```
Session | Staff Code | Name | Position
--------|------------|------|---------------
1       | GV100      | Mr.X | Corridor
2       | GV101      | Ms.Y | Corridor
...     | ...        | ...  | ...
```

---

## 🔧 Configuration

### Server Settings (ServerApp.java)

```java
private static final int PORT          = 8888;      // Listen port
private static final int SO_TIMEOUT_MS = 60_000;    // 60 second timeout
```

### Client Settings (ClientApp.java)

```java
spinTongCanBo.setValue(220);   // Default total staff
spinPhongThi.setValue(100);    // Default exam rooms
spinCa.setValue(1);            // Default sessions
```

---

## 🐛 Troubleshooting

### "Connection refused" Error
- ✅ Ensure server is running
- ✅ Check firewall allows port 8888
- ✅ Verify correct server IP address
- ✅ Ensure client and server are on same network

### "File not found" Error
- ✅ Check file path is correct
- ✅ Verify file exists and is readable
- ✅ Ensure file is `.xlsx` format (not `.xls`)

### "Timeout" Error
- ✅ Check network connectivity
- ✅ Increase timeout value if processing large files
- ✅ Verify server isn't overloaded

### Missing POI Libraries
- ✅ Add Apache POI JAR files to `libs/` folder
- ✅ Update classpath in batch files
- ✅ Rebuild project with Maven

---

## 📈 Performance

| Metric | Value |
|--------|-------|
| Max Staff | 1000+ |
| Max Rooms | 200+ |
| Max Sessions | 20+ |
| Processing Time | <5 seconds for typical load |
| Network Timeout | 60 seconds |

---

## 🤝 Contributing

Contributions are welcome! To contribute:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit changes: `git commit -m 'Add feature'`
4. Push to branch: `git push origin feature/your-feature`
5. Submit a Pull Request

### Development Guidelines
- Follow Java naming conventions
- Add comments for complex logic
- Test with sample data before submitting
- Update this README for new features

---

## 📝 License

This project is open source and available under the [MIT License](LICENSE).

---

## 👤 Author

**DaoQuan03**  
GitHub: [@DaoQuan03](https://github.com/DaoQuan03)

---

## 📞 Support

For issues, questions, or suggestions:
- Open an [Issue](https://github.com/DaoQuan03/exam-invigilator-assignment-system/issues)
- Email: daoquan20032005@gmail.com

---

## 🎓 Educational Use

This project is suitable for learning:
- Client-server architecture patterns
- Socket-based network programming
- Java GUI development with Swing
- Data serialization and object streams
- Excel file processing
- Algorithm design & optimization

---

**Last Updated:** 2026-05-20  
**Version:** 1.0.0
