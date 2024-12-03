# Computer Security-Build It Phase
this client-server application facilitates a basic command protocol between a client and a server, allowing users to register, manipulate a counter (increase/decrease), and log out. The server maintains a simple client database in memory and generates logs of actions. JSON files store client-specific actions and server logs.

---

## Table of Contents

1. [System Requirements](#system-requirements)
2. [Installation](#installation)
3. [Usage](#usage)
4. [Functionality](#functionality)
5. [File Structure](#file-structure)
6. [Error Handling](#error-handling)

---

### System Requirements

- Java Development Kit (JDK) 8 or above

### Installation

1. **Import server certificate into a newly created client trustore**
   Each client needs to create its own truststore, which acts as a repository of certificates the client trust. 

   Run the following command to import the server's certificate into the client's truststore.
   ```bash 
   keytool -import -alias server -file server.crt -keystore client.truststore -storepass YOUR_PASSWORD
   ```

2. **Compile the Java Classes**:
   ```bash
   javac Client.java Server.java
   ```

3. **Run the Server**:
   Open a terminal and run:
   ```bash
   java Server
   ```
   By default, the server listens on port 5001.

4. **Run the Client**:
   Open a new terminal and run:
   ```bash
   java Client
   ```
   Follow the prompts to input the server’s port, client ID, and password.

### Usage

1. **Starting the Server**: Launch the server on a specified port. The server listens for client connections and initiates a `ClientHandler` thread for each client.

2. **Starting the Client**: Connect the client to the server using IP and port. Register by providing a unique client ID and password.

3. **Commands**:
   - **REGISTER**: Automatically sent upon connection to register/login the client.
   - **INCREASE <amount>**: Increase the counter by a specified amount.
   - **DECREASE <amount>**: Decrease the counter by a specified amount.
   - **LOGOUT**: Log out the client and terminate the connection.

### Functionality

1. **Client**:
   - Establishes a connection to the server.
   - Sends commands to the server.
   - Receives and displays responses from the server.

2. **Server**:
   - Manages client registration and connection handling.
   - Processes client commands (`INCREASE`, `DECREASE`, `LOGOUT`).
   - Maintains a counter per client and logs actions in a JSON format.
   - Tracks client-specific actions and stores them in JSON files.
   
### File Structure

- **Client JSON Files**: Each client has a `clientId.JSON` file that stores metadata (e.g., `delay`, `steps`).
- **Log File**: `logfile.JSON` stores a record of each action (timestamp, client ID, action type, amount).

### Error Handling

- The server and client handle various exceptions (e.g., unknown host, socket errors, JSON read/write errors).
- Invalid commands or malformed messages are handled by printing error messages.


### Example Workflow

1. **Register Client**:
   ```
   REGISTER <clientId> <password>
   ```
   Expected response: `ACK: Registration successful.`

2. **Increase Counter**:
   - Enter `INCREASE` and press **Enter**.
   - Then enter the amount to increase, followed by **Enter**.
   - Example response: `Counter increased to <new_value>`

3. **Decrease Counter**:
   - Enter `DECREASE` and press **Enter**.
   - Then enter the amount to decrease, followed by **Enter**.
   - Example response: `Counter decreased to <new_value>`

4. **Logout**:
   ```
   LOGOUT
   ```
   Expected response: `ACK: Logout successful.`

---

This structure allows the user to input commands in stages, following the prompt to enter additional details (like the amount) after the initial command.