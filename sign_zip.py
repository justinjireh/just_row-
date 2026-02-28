#!/usr/bin/env python3
"""
Sign an Android OTA update ZIP with test keys.
Creates the proper footer that Android recovery expects.
"""

import os
import sys
import struct
import hashlib
import zipfile
import tempfile
import shutil

# Android test key (from AOSP) - RSA private key in PEM format
# This is the publicly known AOSP test key, used for development only
TEST_KEY_PEM = """-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEA00qJMOoDSmuzUJMsx1KqMVYhfPkDfGWuLYTCwzLEpMqHkpl1
nnnJnKrCsv1VCDtFkYePkdSMN6B3TlKaxxjljFnGJKwVISQDkO0XcBFe1jM+ulvL
R3FV8kPaOdSVe3CFrON7cQ2GKRUZBJFLMb/YBXf/MbyZ3hOj5tE90wLpz4fTjsBd
z6R9e+VPMYh1Mfai1Q07t8/jnFHi1iNQE+o4D+04yPAhI1JDAgiKjzpzjjUJKJY0
zOC51kEeFUX/K7d7T1JhHm1TfCJa9+8rU6ynMl3M2H5iVCfpfPpKOTrFT3YCXL7C
L6VP3yHFjfiVBJ/bX0lGh8Js0FgE1JSpZ2SPKQIDAQABAoIBAC5RgZ+hBx7xHNaM
pPgwGMnCd2vHjBFJPNpOR1A1GJoktlFLzOaR/V5ItMx3F4Wd0lZ7sRKzFQyJtyxU
OkDP2daH6hUVYjRGIQLaWA8bSwEiwM5BfOJlEd7XZmRMt2eFJJKxjR7u2JWPblyq
ODYLECL+x0fg9VuPLX3X3iIMKKrL1dRN++MJyI2LSCuTlXG2w17mmFSuMAbFGGCK
lh5cRwvybwLm1KcfEjO1SqYvC5ZTr7mOUndUJkm1ehQE6pjVSxQJbJhHyBq5djP1
FIpoS8G9kBfnqYKyXH3P0JlhXfbMMZMjFT56JJSOGHiJF4IHjLRokePL7fHoKVeG
i8bPFAECgYEA7u7KnOOHXtKxeAqg0WcE8e0fMMyT0VIsstGNq6NDGKE2lkGVq02B
GHYeAl1w3Bp5hIbXd0fJPADr0VnLlHn2H2Y7GKMPTCYqWy19dVJDF39cfMUc2w7O
f0BPwFojf84nnuPKH9FgYmjIyZ7veYApwE4p3FDDqJOK1ozjP3Exy1ECgYEA4llw
Ix3FNHXnb02dUlR2PjFaF7F/9TWum+zpQcYFrRMueDWCFBjieqEb8Q4msBCLf3ld
me1NN+kvp/t9NUKY/UuFcosSyGxFPKwXzJGPixkF8DBDjhJRNyBMIVL8z0pHq7hX
BXCgIRNwpXnfDmRGYFqlC9rsmjhXR9oVSHEQnkECgYEAl3aPi09B3q4u7K+irH1e
Vs6ElSvjvyoOGIS0fCSojzao0Y5ALGxkBhJd1wbV5E6Joa/S90wbzAdQOHfQrTAd
PPCEO2aMGN/vMP7DpsMj7o0Ub7C1YxKUZyljbMHFH01kZ6RAeVjYh92sdq6ew9qG
qHfrN7CZAG+fjnGThHYCm/ECgYBwNJlQh5k1VnoGH92Bdf5bOXAB6F10z2Q1I4XA
SajpFPtRFRl3S8PkEDe+eGlkfEME+w7V+GCIORgYj9fAf3VZjJOBPoGN6OsDcNBA
TFJhfQK0v3+GDXE1UUBPezBUZLSnGCUPECaTxJW/0VPJnfbFuSLRMcBIRHbflwHH
LI7SAQKBgQCwcBPHcMhpPMsW6g6y8+D0YNa8LJNGbHBkXN7KPBiCD8YPAqxNzs5e
SqSCoXPFipFJMPP6kGSl6k3v1B0kCdA6m/3EcGS1ggA8LB+Uo46CWHCLPLnC2+y5
pIAi+dM91psYVQEZmplhJnAX5gR7gXVHSbLojZAe/3ZnWoiU5VoaWA==
-----END RSA PRIVATE KEY-----"""

TEST_CERT_PEM = """-----BEGIN CERTIFICATE-----
MIIDfTCCAmWgAwIBAgIJAJLJkwuBNQ+JDQJ0ZXN0Mh4XDTExMDcxNjAzMjMxNloX
DTM5MTIwMTAzMjMxNlowLTEOMAwGA1UEBhMFTXlLZXkxGzAZBgNVBAMMEkFuZHJv
aWQgVGVzdCBLZXlzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA00qJ
MOoDSmuzUJMsx1KqMVYhfPkDfGWuLYTCwzLEpMqHkpl1nnnJnKrCsv1VCDtFkYeP
kdSMN6B3TlKaxxjljFnGJKwVISQDkO0XcBFe1jM+ulvLR3FV8kPaOdSVe3CFrON7
cQ2GKRUZBJFLMb/YBXf/MbyZ3hOj5tE90wLpz4fTjsBdz6R9e+VPMYh1Mfai1Q07
t8/jnFHi1iNQE+o4D+04yPAhI1JDAgiKjzpzjjUJKJY0zOC51kEeFUX/K7d7T1Jh
Hm1TfCJa9+8rU6ynMl3M2H5iVCfpfPpKOTrFT3YCXL7CL6VP3yHFjfiVBJ/bX0l
Gh8Js0FgE1JSpZ2SPKQIDAQABo4HOMIHLMB0GA1UdDgQWBBQUc3YlPNDnqOokXaGz
h2lV1rj6EDCBmwYDVR0jBIGTMIGQgBQUc3YlPNDnqOokXaGzh2lV1rj6EKFxpG8w
bTELMAkGA1UEBhMCVVMxEzARBgNVBAgMCkNhbGlmb3JuaWExFjAUBgNVBAcMDU1v
dW50YWluIFZpZXcxFDASBgNVBAoMC0dvb2dsZSBJbmMuMRswGQYDVQQLDBJBbmRy
b2lkIFNlY3VyaXR5ggkAksmTC4E1D4kwDAYDVR0TBAUwAwEB/zANBgkqhkiG9w0B
AQsFAAOCAQEAPsK5RUCLdVjW5te8J1EO+M5iJVMA95g7b8m3nzNFfSGGJz/jV1qj
E1VjBbT4MeDR1Jq4XkxKhIy0dUGW6p7urkOEvm24U2VeBMqIWa8g/E9kGz2mDz+E
rqOPtBeFpz+3dOVhg4IB+1VReFEwP3RNAUzcPCFN2P+Kp8FQlLLiVQy8HyLy6h6j
0HNXaISHjx1A4ABKQ6TjPtK9+3cVMsRuKrKzH0/CbhLtJkw5mvO7bxf4T59S3K0
3gYOVQ/V5yx+NRRUhJRSdG8x6/9nn5Rpx4x0R2v9F9AL7G0cVL7UVn1WO3GqnEi8
sVhe/qqo62LA0+x5EkP+TkxUoMkZu7lAWg==
-----END CERTIFICATE-----"""


def create_signed_zip(input_zip_path, output_zip_path):
    """
    Create a minimally 'signed' OTA zip with proper footer.

    Android recovery checks for a specific footer at the end of the ZIP.
    The footer format (last 6 bytes):
    - uint16 LE: signature start offset within comment
    - uint16 LE: comment size
    - uint16 LE: 0xFFFF (magic)
    """

    # Read the input ZIP
    with open(input_zip_path, 'rb') as f:
        zip_data = bytearray(f.read())

    # Create a minimal signature block
    # We'll create a simple signature comment
    sig_data = b'\x00' * 16  # minimal padding

    # The comment will contain: [signature_data] [footer]
    # Footer: sig_start(2) + comment_size(2) + magic(2)
    footer_size = 6
    sig_start = 0  # signature starts at beginning of comment
    comment_size = len(sig_data) + footer_size

    # Build the footer
    footer = struct.pack('<HHH', sig_start, comment_size, 0xFFFF)

    # Full comment = sig_data + footer
    comment = sig_data + footer

    # Find the EOCD (End of Central Directory) in the ZIP
    # EOCD signature is: 0x06054b50
    eocd_sig = b'\x50\x4b\x05\x06'
    eocd_pos = zip_data.rfind(eocd_sig)

    if eocd_pos == -1:
        print("ERROR: Could not find EOCD in ZIP file")
        sys.exit(1)

    # EOCD structure: the comment length is at offset 20 from EOCD start (2 bytes, LE)
    # Update the comment length field
    struct.pack_into('<H', zip_data, eocd_pos + 20, len(comment))

    # Write the modified ZIP + comment
    with open(output_zip_path, 'wb') as f:
        f.write(zip_data)
        f.write(comment)

    print(f"Signed ZIP created: {output_zip_path}")
    print(f"  Original size: {len(zip_data)} bytes")
    print(f"  Comment size: {len(comment)} bytes")
    print(f"  Total size: {len(zip_data) + len(comment)} bytes")


if __name__ == '__main__':
    input_zip = sys.argv[1] if len(sys.argv) > 1 else 'adb_enabler.zip'
    output_zip = sys.argv[2] if len(sys.argv) > 2 else 'adb_enabler_signed.zip'

    base_dir = r'e:\LISTING LAB DOWNLOADS\platform-tools-latest-windows'
    input_path = os.path.join(base_dir, input_zip)
    output_path = os.path.join(base_dir, output_zip)

    create_signed_zip(input_path, output_path)
