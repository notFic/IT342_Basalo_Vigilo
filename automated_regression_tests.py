import unittest
import requests
import uuid
import os

BASE_URL = "http://localhost:8080/api/v1"

class TestVigiloSystem(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        # Create unique credentials for testing
        cls.admin_email = f"admin_{uuid.uuid4().hex[:6]}@vigilo.local"
        cls.admin_password = "adminpassword123"
        
        cls.staff_email = f"staff_{uuid.uuid4().hex[:6]}@vigilo.local"
        cls.staff_password = "staffpassword123"
        
        cls.test_log_id_checkout = None
        cls.test_log_id_void = None
        
        # Create a dummy image
        cls.dummy_img_path = "test_dummy_id.png"
        with open(cls.dummy_img_path, "wb") as f:
            f.write(b"dummy image data")

    @classmethod
    def tearDownClass(cls):
        if os.path.exists(cls.dummy_img_path):
            os.remove(cls.dummy_img_path)

    # ==========================
    # AUTHENTICATION SLICE
    # ==========================
    
    def test_01_AUTH_01_staff_registration(self):
        """AUTH-01: Verify system can register new staff"""
        url = f"{BASE_URL}/auth/register"
        payload = {
            "email": self.staff_email,
            "password": self.staff_password,
            "firstName": "Staff",
            "lastName": "User",
            "role": "STAFF"
        }
        response = requests.post(url, json=payload)
        self.assertEqual(response.status_code, 201)
        self.assertTrue(response.json().get("success"))

    def test_02_AUTH_02_admin_login(self):
        """AUTH-02: Verify system can authenticate Admin credentials"""
        # Register the admin first to ensure it exists
        requests.post(f"{BASE_URL}/auth/register", json={
            "email": self.admin_email,
            "password": self.admin_password,
            "firstName": "Admin",
            "lastName": "User",
            "role": "ADMIN"
        })
        url = f"{BASE_URL}/auth/login"
        payload = {
            "email": self.admin_email,
            "password": self.admin_password
        }
        response = requests.post(url, json=payload)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["data"]["role"], "ADMIN")

    def test_03_AUTH_03_staff_login_web(self):
        """AUTH-03: Verify system can authenticate Staff credentials"""
        url = f"{BASE_URL}/auth/login"
        payload = {
            "email": self.staff_email,
            "password": self.staff_password
        }
        response = requests.post(url, json=payload)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["data"]["role"], "STAFF")

    def test_04_AUTH_05_invalid_credentials(self):
        """AUTH-05: Verify system rejects incorrect passwords"""
        url = f"{BASE_URL}/auth/login"
        payload = {
            "email": self.admin_email,
            "password": "wrongpassword123!"
        }
        response = requests.post(url, json=payload)
        self.assertEqual(response.status_code, 400)
        self.assertFalse(response.json().get("success"))

    # ==========================
    # VISITOR SLICE
    # ==========================

    def test_05_VIS_01_new_check_in_web(self):
        """VIS-01: Verify complete visitor check-in form submission"""
        url = f"{BASE_URL}/logs/check-in"
        files = {"idImage": ("test_dummy_id.png", open(self.dummy_img_path, "rb"), "image/png")}
        data = {
            "fullName": "Jack Sparrow",
            "contactNumber": "09171234567",
            "hostName": "Hector Barbossa",
            "visitorType": "Guest",
            "destinationRoom": "404 - Main Bldg.",
            "purpose": "Piracy Meeting",
            "extendedVisit": "false",
            "createdByEmail": self.staff_email
        }
        response = requests.post(url, data=data, files=files)
        self.assertEqual(response.status_code, 200)
        
        TestVigiloSystem.test_log_id_checkout = response.json().get("id")
        self.assertIsNotNone(TestVigiloSystem.test_log_id_checkout)

    def test_06_VIS_03_check_out_process(self):
        """VIS-03: Verify check-out moves active visitor to history"""
        self.assertIsNotNone(self.test_log_id_checkout)
        url = f"{BASE_URL}/logs/{self.test_log_id_checkout}/check-out?updatedByEmail={self.staff_email}"
        response = requests.put(url)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json().get("status"), "Checked-Out")

    def test_07_VIS_04_void_record(self):
        """VIS-04: Verify voiding an active record"""
        # Create a fresh record just to void it
        url_checkin = f"{BASE_URL}/logs/check-in"
        files = {"idImage": ("test_dummy_id.png", open(self.dummy_img_path, "rb"), "image/png")}
        data = {
            "fullName": "Mistake Visitor",
            "contactNumber": "09171234567",
            "hostName": "Hector Barbossa",
            "visitorType": "Guest",
            "destinationRoom": "404",
            "purpose": "Mistake",
            "extendedVisit": "false",
            "createdByEmail": self.staff_email
        }
        response_in = requests.post(url_checkin, data=data, files=files)
        void_id = response_in.json().get("id")
        
        # Void the record as Admin
        url_void = f"{BASE_URL}/logs/{void_id}/void?updatedByEmail={self.admin_email}"
        response_void = requests.put(url_void)
        self.assertEqual(response_void.status_code, 200)
        self.assertEqual(response_void.json().get("status"), "Voided")

    def test_08_VIS_05_validation_rules(self):
        """VIS-05: Verify form rejects submission without ID image"""
        url = f"{BASE_URL}/logs/check-in"
        data = {
            "fullName": "No Image Hacker",
            "contactNumber": "09171234567",
            "hostName": "Admin",
            "visitorType": "Guest",
            "destinationRoom": "Lobby",
            "purpose": "Hacking",
            "extendedVisit": "false",
            "createdByEmail": self.staff_email
        }
        # Send empty file to force multipart/form-data
        files = {"idImage": ("", "")}
        response = requests.post(url, data=data, files=files)
        self.assertEqual(response.status_code, 400)
        # Should throw "Required part 'idImage' is not present." from Spring Boot

    # ==========================
    # ADMIN / SYSTEM SLICE
    # ==========================

    def test_09_ADM_01_location_management(self):
        """ADM-01: Verify system locations are retrievable"""
        url = f"{BASE_URL}/locations"
        response = requests.get(url)
        self.assertEqual(response.status_code, 200)
        self.assertTrue(isinstance(response.json(), list))

    def test_10_ADM_02_staff_management_view(self):
        """ADM-02: Verify registered staff list view"""
        url = f"{BASE_URL}/users"
        response = requests.get(url)
        self.assertEqual(response.status_code, 200)
        users = response.json()
        self.assertTrue(len(users) > 0)
        self.assertIn("role", users[0])

    def test_11_ADM_03_audit_log_generation(self):
        """ADM-03: Verify system records actions in audit log"""
        url = f"{BASE_URL}/audit"
        response = requests.get(url)
        self.assertEqual(response.status_code, 200)
        audit_logs = response.json()
        self.assertTrue(len(audit_logs) > 0)

if __name__ == "__main__":
    print("=" * 70)
    print("VIGILO VERTICAL SLICE ARCHITECTURE - FULL REGRESSION TEST SUITE")
    print("=" * 70)
    unittest.main(verbosity=2)
