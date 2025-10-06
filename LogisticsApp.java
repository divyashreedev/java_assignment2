import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class LogisticsApp {

    static class Customer {
        final String id;
        final String name;
        final String address;

        Customer(String id, String name, String address) {
            this.id = id;
            this.name = name;
            this.address = address;
        }

        @Override
        public String toString() {
            return String.format("%s (%s) - %s", id, name, address);
        }
    }

    static class Hub {
        final String id;
        final String name;

        Hub(String id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return String.format("%s - %s", id, name);
        }
    }

    enum ParcelLifecycleStatus {
        CREATED,
        IN_TRANSIT,
        OUT_FOR_DELIVERY,
        DELIVERED,
        RETURNED,
        DELIVERY_FAILED
    }

    static class ScanEvent {
        final Hub hub;
        final LocalDateTime timestamp;
        final String note; // optional

        ScanEvent(Hub hub, String note) {
            this.hub = hub;
            this.timestamp = LocalDateTime.now();
            this.note = note;
        }

        @Override
        public String toString() {
            return String.format("%s @ %s%s",
                    hub.name,
                    timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    (note == null || note.isEmpty()) ? "" : " (" + note + ")");
        }
    }

    static class DeliveryAttempt {
        final LocalDateTime timestamp;
        final boolean success;
        final String outcomeNote; // e.g. "No one home", "Address not found"
        final String attemptedBy; // optional courier name/id

        DeliveryAttempt(boolean success, String outcomeNote, String attemptedBy) {
            this.timestamp = LocalDateTime.now();
            this.success = success;
            this.outcomeNote = outcomeNote;
            this.attemptedBy = attemptedBy;
        }

        @Override
        public String toString() {
            String s = String.format("%s at %s",
                    success ? "SUCCESS" : "FAILED",
                    timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            if (outcomeNote != null && !outcomeNote.isEmpty()) s += " - " + outcomeNote;
            if (attemptedBy != null && !attemptedBy.isEmpty()) s += " (by " + attemptedBy + ")";
            return s;
        }
    }

    static class ProofOfDelivery {
        final String receiverName;
        final String codeOrSignature; // placeholder for signature/code
        final LocalDateTime timestamp;

        ProofOfDelivery(String receiverName, String codeOrSignature) {
            this.receiverName = receiverName;
            this.codeOrSignature = codeOrSignature;
            this.timestamp = LocalDateTime.now();
        }

        @Override
        public String toString() {
            return String.format("Received by %s @ %s (Proof: %s)",
                    receiverName,
                    timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    codeOrSignature);
        }
    }

    static class Parcel {
        final String id;
        final Customer sender;
        final Customer receiver;
        final double weightKg;
        ParcelLifecycleStatus status;
        final List<ScanEvent> scans = new ArrayList<>();
        final List<DeliveryAttempt> deliveryAttempts = new ArrayList<>();
        ProofOfDelivery proof = null;
        String lastKnownHubId = null;

        Parcel(String id, Customer sender, Customer receiver, double weightKg) {
            this.id = id;
            this.sender = sender;
            this.receiver = receiver;
            this.weightKg = weightKg;
            this.status = ParcelLifecycleStatus.CREATED;
        }

        void addScan(ScanEvent se) {
            scans.add(se);
            lastKnownHubId = se.hub.id;
            // change status to IN_TRANSIT when scanned at intermediate hub
            if (status == ParcelLifecycleStatus.CREATED || status == ParcelLifecycleStatus.DELIVERY_FAILED)
                status = ParcelLifecycleStatus.IN_TRANSIT;
        }

        void addDeliveryAttempt(DeliveryAttempt da) {
            deliveryAttempts.add(da);
            if (da.success) {
                status = ParcelLifecycleStatus.DELIVERED;
            } else {
                status = ParcelLifecycleStatus.DELIVERY_FAILED;
            }
        }

        void addProof(ProofOfDelivery p) {
            this.proof = p;
            if (p != null) status = ParcelLifecycleStatus.DELIVERED;
        }

        void markReturned(String reasonNote) {
            status = ParcelLifecycleStatus.RETURNED;
            // add a scan-like entry to note return - as a scan with a hub named "RETURN"
            scans.add(new ScanEvent(new Hub("RETURN", "Returned to Sender"), reasonNote));
        }

        String statusSummary() {
            return status.toString();
        }

        void printTimeline() {
            System.out.println("-------------------------------------------------");
            System.out.println("Parcel ID: " + id);
            System.out.println("Sender: " + sender);
            System.out.println("Receiver: " + receiver);
            System.out.printf("Weight: %.2f kg\n", weightKg);
            System.out.println("Current status: " + statusSummary());
            System.out.println();
            System.out.println("Scan history:");
            if (scans.isEmpty()) System.out.println("  (no scans recorded)");
            else {
                for (ScanEvent s : scans) System.out.println("  - " + s);
            }
            System.out.println();
            System.out.println("Delivery attempts:");
            if (deliveryAttempts.isEmpty()) System.out.println("  (no delivery attempts)");
            else {
                for (DeliveryAttempt d : deliveryAttempts) System.out.println("  - " + d);
            }
            System.out.println();
            if (proof != null) {
                System.out.println("Proof of Delivery:");
                System.out.println("  - " + proof);
            }
            System.out.println("-------------------------------------------------");
        }
    }

    static class Shipment {
        final String id;
        final List<Parcel> parcels = new ArrayList<>();
        final LocalDateTime createdAt = LocalDateTime.now();

        Shipment(String id) {
            this.id = id;
        }

        void addParcel(Parcel p) {
            parcels.add(p);
        }

        boolean isClosable() {
            // closed when every parcel is DELIVERED or RETURNED
            for (Parcel p : parcels) {
                if (!(p.status == ParcelLifecycleStatus.DELIVERED || p.status == ParcelLifecycleStatus.RETURNED)) {
                    return false;
                }
            }
            return true;
        }

        void printSummary() {
            System.out.println("=================================================");
            System.out.println("Shipment ID: " + id);
            System.out.println("Created: " + createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            System.out.println("Parcels:");
            for (Parcel p : parcels) {
                System.out.printf("  - %s : %s\n", p.id, p.statusSummary());
            }
            System.out.println("Shipment closable/closed: " + isClosable());
            System.out.println("=================================================");
        }
    }


    /* ---------- Application state ---------- */

    private final Map<String, Customer> customers = new HashMap<>();
    private final Map<String, Hub> hubs = new HashMap<>();
    private final Map<String, Parcel> parcels = new HashMap<>();
    private final Map<String, Shipment> shipments = new HashMap<>();
    private final Scanner scanner = new Scanner(System.in);

    /* ---------- Console UI & actions ---------- */

    private void run() {
        System.out.println("=== Logistics & Parcel Tracking Console App ===");
        boolean running = true;
        while (running) {
            printMenu();
            String choice = prompt("Enter choice");
            switch (choice) {
                case "1": createCustomer(); break;
                case "2": createParcel(); break;
                case "3": createShipment(); break;
                case "4": addHub(); break;
                case "5": recordScan(); break;
                case "6": recordDeliveryAttempt(); break;
                case "7": showParcelStatus(); break;
                case "8": showShipmentSummary(); break;
                case "9": markParcelReturned(); break;
                case "0": running = false; break;
                default: System.out.println("Invalid choice. Try again."); break;
            }
        }
        System.out.println("Exiting. Goodbye!");
    }

    private void printMenu() {
        System.out.println();
        System.out.println("Menu:");
        System.out.println(" 1. Create Customer");
        System.out.println(" 2. Create Parcel");
        System.out.println(" 3. Create Shipment");
        System.out.println(" 4. Add Hub");
        System.out.println(" 5. Record Scan (parcel -> hub)");
        System.out.println(" 6. Record Delivery Attempt");
        System.out.println(" 7. Show Parcel Status / Timeline");
        System.out.println(" 8. Show Shipment Summary");
        System.out.println(" 9. Mark Parcel Returned");
        System.out.println(" 0. Exit");
    }

    /* ---------- Handlers ---------- */

    private void createCustomer() {
        String id = promptNonEmpty("Customer ID");
        if (customers.containsKey(id)) {
            System.out.println("Customer ID already exists.");
            return;
        }
        String name = promptNonEmpty("Name");
        String addr = promptNonEmpty("Address");
        customers.put(id, new Customer(id, name, addr));
        System.out.println("Customer created: " + id);
    }

    private void createParcel() {
        String id = promptNonEmpty("Parcel ID");
        if (parcels.containsKey(id)) {
            System.out.println("Parcel ID already exists.");
            return;
        }
        if (customers.isEmpty()) {
            System.out.println("No customers exist. Create sender/receiver first.");
            return;
        }
        System.out.println("Available customers:");
        customers.forEach((k, v) -> System.out.println("  " + k + " -> " + v.name));
        String sid = promptNonEmpty("Sender ID");
        String rid = promptNonEmpty("Receiver ID");
        if (!customers.containsKey(sid) || !customers.containsKey(rid)) {
            System.out.println("Invalid sender or receiver ID.");
            return;
        }
        double weight = promptDouble("Weight (kg)");
        Parcel p = new Parcel(id, customers.get(sid), customers.get(rid), weight);
        parcels.put(id, p);
        System.out.println("Parcel created: " + id);
    }

    private void createShipment() {
        String id = promptNonEmpty("Shipment ID");
        if (shipments.containsKey(id)) {
            System.out.println("Shipment ID already exists.");
            return;
        }
        Shipment s = new Shipment(id);
        System.out.println("Enter parcel IDs to add to shipment (comma-separated), or leave empty to add later:");
        String line = prompt("");
        if (!line.trim().isEmpty()) {
            String[] parts = line.split(",");
            for (String pid : parts) {
                pid = pid.trim();
                Parcel p = parcels.get(pid);
                if (p != null) {
                    s.addParcel(p);
                } else {
                    System.out.println("  (skipped) parcel not found: " + pid);
                }
            }
        }
        shipments.put(id, s);
        System.out.println("Shipment created: " + id);
    }

    private void addHub() {
        String id = promptNonEmpty("Hub ID");
        if (hubs.containsKey(id)) {
            System.out.println("Hub ID already exists.");
            return;
        }
        String name = promptNonEmpty("Hub name");
        hubs.put(id, new Hub(id, name));
        System.out.println("Hub added: " + id + " - " + name);
    }

    private void recordScan() {
        if (parcels.isEmpty()) {
            System.out.println("No parcels available.");
            return;
        }
        String pid = promptNonEmpty("Parcel ID");
        Parcel p = parcels.get(pid);
        if (p == null) {
            System.out.println("Parcel not found: " + pid);
            return;
        }
        if (hubs.isEmpty()) {
            System.out.println("No hubs available. Add a hub first.");
            return;
        }
        System.out.println("Available hubs:");
        hubs.forEach((k, v) -> System.out.println("  " + k + " -> " + v.name));
        String hid = promptNonEmpty("Hub ID");
        Hub hub = hubs.get(hid);
        if (hub == null) {
            System.out.println("Hub not found: " + hid);
            return;
        }
        String note = prompt("Optional note for scan (e.g., 'Arrived', 'Departed')");
        ScanEvent se = new ScanEvent(hub, note);
        p.addScan(se);
        System.out.println("Scan recorded for parcel " + pid + " at hub " + hub.name + ".");
    }

    private void recordDeliveryAttempt() {
        if (parcels.isEmpty()) {
            System.out.println("No parcels available.");
            return;
        }
        String pid = promptNonEmpty("Parcel ID");
        Parcel p = parcels.get(pid);
        if (p == null) {
            System.out.println("Parcel not found: " + pid);
            return;
        }
        String by = prompt("Courier / attempted by (optional)");
        String succ = promptNonEmpty("Was delivery successful? (y/n)").trim().toLowerCase();
        boolean success = succ.equals("y") || succ.equals("yes");
        String note = prompt("Outcome note (e.g., 'No one home', 'Address wrong')");
        DeliveryAttempt da = new DeliveryAttempt(success, note, by);
        p.addDeliveryAttempt(da);

        if (success) {
            System.out.println("Delivery marked SUCCESS. Proof of delivery required.");
            String receiverName = promptNonEmpty("Recipient name for proof");
            String code = promptNonEmpty("Proof code/signature placeholder");
            ProofOfDelivery pod = new ProofOfDelivery(receiverName, code);
            p.addProof(pod);
            System.out.println("Proof collected and parcel marked DELIVERED.");
        } else {
            System.out.println("Delivery attempt recorded as FAILED. Parcel status updated.");
        }
    }

    private void showParcelStatus() {
        String pid = promptNonEmpty("Parcel ID");
        Parcel p = parcels.get(pid);
        if (p == null) {
            System.out.println("Parcel not found: " + pid);
            return;
        }
        p.printTimeline();
    }

    private void showShipmentSummary() {
        String sid = promptNonEmpty("Shipment ID");
        Shipment s = shipments.get(sid);
        if (s == null) {
            System.out.println("Shipment not found: " + sid);
            return;
        }
        s.printSummary();
        if (!s.parcels.isEmpty()) {
            if (s.isClosable()) {
                System.out.println("-> All parcels delivered or returned. Shipment can be closed.");
            } else {
                System.out.println("-> Shipment cannot be closed: outstanding parcels remain.");
            }
        }
    }

    private void markParcelReturned() {
        String pid = promptNonEmpty("Parcel ID");
        Parcel p = parcels.get(pid);
        if (p == null) {
            System.out.println("Parcel not found: " + pid);
            return;
        }
        String reason = prompt("Return reason/note (optional)");
        p.markReturned(reason);
        System.out.println("Parcel " + pid + " marked as RETURNED.");
    }

    /* ---------- Utilities ---------- */

    private String prompt(String message) {
        if (message == null || message.isEmpty()) {
            System.out.print("> ");
        } else {
            System.out.print(message + ": ");
        }
        return scanner.nextLine().trim();
    }

    private String promptNonEmpty(String message) {
        while (true) {
            String v = prompt(message);
            if (!v.isEmpty()) return v;
            System.out.println("Input cannot be empty.");
        }
    }

    private double promptDouble(String message) {
        while (true) {
            String v = prompt(message);
            try {
                return Double.parseDouble(v);
            } catch (NumberFormatException ex) {
                System.out.println("Enter a valid number (e.g., 2.5).");
            }
        }
    }

    /* ---------- Main ---------- */

    public static void main(String[] args) {
        LogisticsApp app = new LogisticsApp();
        // Optional: add sample data to help quick testing
        app.bootstrapSampleData();
        app.run();
    }

    private void bootstrapSampleData() {
        // Preload one customer, hub, and parcel to make initial testing easier.
        Customer c1 = new Customer("C001", "Alice", "12 Park Street, Chennai");
        Customer c2 = new Customer("C002", "Bob", "45 Lake Road, Coimbatore");
        customers.put(c1.id, c1);
        customers.put(c2.id, c2);

        Hub h1 = new Hub("H001", "Chennai Hub");
        Hub h2 = new Hub("H002", "Coimbatore Hub");
        hubs.put(h1.id, h1);
        hubs.put(h2.id, h2);

        Parcel p1 = new Parcel("P001", c1, c2, 1.2);
        parcels.put(p1.id, p1);

        Shipment sh = new Shipment("S001");
        sh.addParcel(p1);
        shipments.put(sh.id, sh);

        System.out.println("Sample data loaded: customers C001/C002, hubs H001/H002, parcel P001, shipment S001");
    }
}
