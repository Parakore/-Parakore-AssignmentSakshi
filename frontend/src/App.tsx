import { useState } from "react";
import type { FormEvent } from "react";
import "./App.css";

const API_BASE = "http://localhost:8080/rcp/v1";

type Application = {
  id: number;
  applicationNumber: string;
  tenantId: string;
  applicantUuid: string;
  applicantMobile: string;
  roadType: string;
  lengthInMeters: number;
  widthInMeters: number;
  durationInDays: number;
  applicantType: string;
  proposedStartDate: string;
  areaInSqm: number;
  restorationCharge: number;
  permissionFee: number;
  urgencySurcharge: number;
  securityDeposit: number;
  totalAmount: number;
  status: string;
};

function App() {
  const [activeTab, setActiveTab] = useState<"create" | "search">("create");

  const [tenantId, setTenantId] = useState("dehradun");
  const [mobileNumber, setMobileNumber] = useState("9990000001");
  const [applicantUuid, setApplicantUuid] = useState("user-001");
  const [roadType, setRoadType] = useState("BT");
  const [length, setLength] = useState("10");
  const [width, setWidth] = useState("5");
  const [duration, setDuration] = useState("2");
  const [applicantType, setApplicantType] = useState("PRIVATE");
  const [startDate, setStartDate] = useState("");

  const [searchNumber, setSearchNumber] = useState("");
  const [searchStatus, setSearchStatus] = useState("");
  const [searchMobile, setSearchMobile] = useState("");

  const [applications, setApplications] = useState<Application[]>([]);
  const [createdApplication, setCreatedApplication] =
    useState<Application | null>(null);

  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  function clearMessages() {
    setMessage("");
    setError("");
  }

  async function createApplication(event: FormEvent) {
    event.preventDefault();
    clearMessages();
    setLoading(true);
    setCreatedApplication(null);

    try {
      const today = new Date().toISOString().split("T")[0];

      const request = {
        RequestInfo: {
          apiId: "rcp",
          msgId: `web-${Date.now()}`,
          userInfo: {
            uuid: applicantUuid,
            userName: mobileNumber,
            tenantId,
            roles: [{ code: "APPLICANT" }],
          },
        },
        Calculation: {
          tenantId,
          roadType,
          lengthInMeters: Number(length),
          widthInMeters: Number(width),
          durationInDays: Number(duration),
          applicantType,
          proposedStartDate: startDate,
          applicationDate: today,
        },
      };

      const response = await fetch(`${API_BASE}/_create`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(request),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(
          data?.Errors?.[0]?.message || "Failed to create application"
        );
      }

      setCreatedApplication(data.Application);
      setMessage(
        `Application created successfully: ${data.Application.applicationNumber}`
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : "Something went wrong");
    } finally {
      setLoading(false);
    }
  }

  async function searchApplications(event: FormEvent) {
    event.preventDefault();
    clearMessages();
    setLoading(true);
    setApplications([]);

    try {
      const request = {
        RequestInfo: {
          apiId: "rcp",
          msgId: `search-${Date.now()}`,
          userInfo: {
            uuid: applicantUuid,
            userName: mobileNumber,
            tenantId,
            roles: [{ code: "APPLICANT" }],
          },
        },
        applicationNumber: searchNumber || null,
        status: searchStatus || null,
        mobileNumber: searchMobile || null,
        applicantUuid: null,
        offset: 0,
        limit: 20,
      };

      const response = await fetch(`${API_BASE}/_search`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(request),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(
          data?.Errors?.[0]?.message || "Failed to search applications"
        );
      }

      setApplications(data.Applications || []);
      setMessage(
        `Search completed. ${data.totalCount ?? 0} application(s) found.`
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : "Something went wrong");
    } finally {
      setLoading(false);
    }
  }

  async function performAction(
    applicationNumber: string,
    action: string,
    role: string
  ) {
    clearMessages();
    setLoading(true);

    try {
      const request = {
        RequestInfo: {
          apiId: "rcp",
          msgId: `action-${Date.now()}`,
          userInfo: {
            uuid: applicantUuid,
            userName: mobileNumber,
            tenantId,
            roles: [{ code: role }],
          },
        },
        applicationNumber,
        action,
        comment: `${action} from web portal`,
      };

      const response = await fetch(`${API_BASE}/_action`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(request),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(
          data?.Errors?.[0]?.message || `Failed to perform ${action}`
        );
      }

      setMessage(`Action ${action} completed successfully.`);

      // Refresh search results after an action.
      if (activeTab === "search") {
        await searchApplications(
          new Event("submit") as unknown as FormEvent
        );
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Something went wrong");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="app">
      <header className="header">
        <div>
          <h1>Road Cutting Permission</h1>
          <p>Application Management Portal</p>
        </div>

        <div className="tenant">
          Tenant: <strong>{tenantId}</strong>
        </div>
      </header>

      <main className="container">
        <div className="tabs">
          <button
            className={activeTab === "create" ? "tab active" : "tab"}
            onClick={() => {
              setActiveTab("create");
              clearMessages();
            }}
          >
            Create Application
          </button>

          <button
            className={activeTab === "search" ? "tab active" : "tab"}
            onClick={() => {
              setActiveTab("search");
              clearMessages();
            }}
          >
            Search Applications
          </button>
        </div>

        {message && <div className="success">{message}</div>}
        {error && <div className="error">{error}</div>}

        {activeTab === "create" && (
          <section className="card">
            <h2>Create Road Cutting Application</h2>

            <form onSubmit={createApplication}>
              <div className="grid">
                <div className="field">
                  <label>Tenant</label>
                  <select
                    value={tenantId}
                    onChange={(e) => setTenantId(e.target.value)}
                  >
                    <option value="dehradun">Dehradun</option>
                    <option value="haridwar">Haridwar</option>
                  </select>
                </div>

                <div className="field">
                  <label>Applicant UUID</label>
                  <input
                    value={applicantUuid}
                    onChange={(e) => setApplicantUuid(e.target.value)}
                    required
                  />
                </div>

                <div className="field">
                  <label>Mobile Number</label>
                  <input
                    value={mobileNumber}
                    onChange={(e) => setMobileNumber(e.target.value)}
                    required
                  />
                </div>

                <div className="field">
                  <label>Road Type</label>
                  <select
                    value={roadType}
                    onChange={(e) => setRoadType(e.target.value)}
                  >
                    <option value="BT">Bituminous</option>
                    <option value="CC">Cement Concrete</option>
                    <option value="WBM">Water Bound Macadam</option>
                  </select>
                </div>

                <div className="field">
                  <label>Length (meters)</label>
                  <input
                    type="number"
                    min="0.001"
                    step="0.001"
                    value={length}
                    onChange={(e) => setLength(e.target.value)}
                    required
                  />
                </div>

                <div className="field">
                  <label>Width (meters)</label>
                  <input
                    type="number"
                    min="0.001"
                    step="0.001"
                    value={width}
                    onChange={(e) => setWidth(e.target.value)}
                    required
                  />
                </div>

                <div className="field">
                  <label>Duration (days)</label>
                  <input
                    type="number"
                    min="1"
                    value={duration}
                    onChange={(e) => setDuration(e.target.value)}
                    required
                  />
                </div>

                <div className="field">
                  <label>Applicant Type</label>
                  <select
                    value={applicantType}
                    onChange={(e) => setApplicantType(e.target.value)}
                  >
                    <option value="PRIVATE">Private</option>
                    <option value="GOVERNMENT_AGENCY">
                      Government Agency
                    </option>
                  </select>
                </div>

                <div className="field">
                  <label>Proposed Start Date</label>
                  <input
                    type="date"
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                    required
                  />
                </div>
              </div>

              <button className="primary" type="submit" disabled={loading}>
                {loading ? "Creating..." : "Create Application"}
              </button>
            </form>
          </section>
        )}

        {createdApplication && (
          <section className="card result">
            <h2>Application Created</h2>

            <div className="application-number">
              {createdApplication.applicationNumber}
            </div>

            <div className="details">
              <div>
                <span>Status</span>
                <strong>{createdApplication.status}</strong>
              </div>

              <div>
                <span>Area</span>
                <strong>{createdApplication.areaInSqm} sqm</strong>
              </div>

              <div>
                <span>Restoration Charge</span>
                <strong>
                  ₹{createdApplication.restorationCharge.toLocaleString()}
                </strong>
              </div>

              <div>
                <span>Permission Fee</span>
                <strong>
                  ₹{createdApplication.permissionFee.toLocaleString()}
                </strong>
              </div>

              <div>
                <span>Urgency Surcharge</span>
                <strong>
                  ₹{createdApplication.urgencySurcharge.toLocaleString()}
                </strong>
              </div>

              <div>
                <span>Security Deposit</span>
                <strong>
                  ₹{createdApplication.securityDeposit.toLocaleString()}
                </strong>
              </div>

              <div className="total">
                <span>Total Amount</span>
                <strong>
                  ₹{createdApplication.totalAmount.toLocaleString()}
                </strong>
              </div>
            </div>
          </section>
        )}

        {activeTab === "search" && (
          <section className="card">
            <h2>Search Applications</h2>

            <form onSubmit={searchApplications}>
              <div className="grid">
                <div className="field">
                  <label>Application Number</label>
                  <input
                    value={searchNumber}
                    onChange={(e) => setSearchNumber(e.target.value)}
                    placeholder="DEH-RCP-000001-2026-27"
                  />
                </div>

                <div className="field">
                  <label>Status</label>
                  <select
                    value={searchStatus}
                    onChange={(e) => setSearchStatus(e.target.value)}
                  >
                    <option value="">All</option>
                    <option value="APPLIED">APPLIED</option>
                    <option value="PENDING_APPROVAL">
                      PENDING_APPROVAL
                    </option>
                    <option value="APPROVED">APPROVED</option>
                    <option value="REJECTED">REJECTED</option>
                    <option value="CANCELLED">CANCELLED</option>
                  </select>
                </div>

                <div className="field">
                  <label>Mobile Number</label>
                  <input
                    value={searchMobile}
                    onChange={(e) => setSearchMobile(e.target.value)}
                    placeholder="9990000001"
                  />
                </div>
              </div>

              <button className="primary" type="submit" disabled={loading}>
                {loading ? "Searching..." : "Search"}
              </button>
            </form>

            {applications.length > 0 && (
              <div className="table-wrapper">
                <table>
                  <thead>
                    <tr>
                      <th>Application</th>
                      <th>Road Type</th>
                      <th>Area</th>
                      <th>Total</th>
                      <th>Status</th>
                      <th>Actions</th>
                    </tr>
                  </thead>

                  <tbody>
                    {applications.map((application) => (
                      <tr key={application.id}>
                        <td>{application.applicationNumber}</td>
                        <td>{application.roadType}</td>
                        <td>{application.areaInSqm} sqm</td>
                        <td>
                          ₹{application.totalAmount.toLocaleString()}
                        </td>
                        <td>
                          <span className="status">
                            {application.status}
                          </span>
                        </td>
                        <td>
                          <div className="actions">
                            {application.status === "APPLIED" && (
                              <>
                                <button
                                  onClick={() =>
                                    performAction(
                                      application.applicationNumber,
                                      "VERIFY",
                                      "VERIFIER"
                                    )
                                  }
                                >
                                  Verify
                                </button>

                                <button
                                  className="danger"
                                  onClick={() =>
                                    performAction(
                                      application.applicationNumber,
                                      "CANCEL",
                                      "APPLICANT"
                                    )
                                  }
                                >
                                  Cancel
                                </button>
                              </>
                            )}

                            {application.status === "PENDING_APPROVAL" && (
                              <>
                                <button
                                  onClick={() =>
                                    performAction(
                                      application.applicationNumber,
                                      "APPROVE",
                                      "APPROVER"
                                    )
                                  }
                                >
                                  Approve
                                </button>

                                <button
                                  className="danger"
                                  onClick={() =>
                                    performAction(
                                      application.applicationNumber,
                                      "REJECT",
                                      "APPROVER"
                                    )
                                  }
                                >
                                  Reject
                                </button>

                                <button
                                  onClick={() =>
                                    performAction(
                                      application.applicationNumber,
                                      "SEND_BACK",
                                      "VERIFIER"
                                    )
                                  }
                                >
                                  Send Back
                                </button>
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {!loading && applications.length === 0 && (
              <p className="empty">No applications found.</p>
            )}
          </section>
        )}
      </main>
    </div>
  );
}

export default App;

