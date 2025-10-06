# 📦 Logistics & Parcel Tracking Console App

### 🧾 Overview
This Java console application simulates a **parcel logistics management system**.  
It allows managing **customers, parcels, shipments, hubs, scan events, and delivery attempts**, ensuring full traceability from dispatch to delivery or return.

---

### ⚙️ Features
- Create and manage:
  - **Customers** (Sender & Receiver)
  - **Parcels**
  - **Shipments**
  - **Hubs**
- Record real-time:
  - **Scan events** (parcel movement between hubs)
  - **Delivery attempts** and **proof of delivery**
- **Status tracking** per parcel with complete timeline
- **Shipment summaries** showing if all parcels are delivered or returned
- Simple **menu-driven console interface**

---

### 🧩 Business Rules Implemented
1. Every **ScanEvent** updates a parcel’s status and location.  
2. **DeliveryAttempt** records successful or failed attempts.  
3. A successful delivery requires a **ProofOfDelivery** (receiver name & code).  
4. A **Shipment** cannot be closed until **all parcels** are either *Delivered* or *Returned*.  
5. **Returned parcels** are logged with reason and final hub “Returned to Sender”.

---

### 🧱 Core Classes
| Class | Description |
|-------|--------------|
| `Customer` | Represents sender or receiver information |
| `Hub` | Represents a logistics hub location |
| `Parcel` | Contains parcel details, scan history, delivery attempts, proof of delivery |
| `Shipment` | Groups multiple parcels for shipment tracking |
| `ScanEvent` | Records parcel scans at specific hubs |
| `DeliveryAttempt` | Logs delivery success/failure with notes |
| `ProofOfDelivery` | Captures confirmation details after successful delivery |
| `LogisticsApp` | Main class handling user interaction and data storage |

---
