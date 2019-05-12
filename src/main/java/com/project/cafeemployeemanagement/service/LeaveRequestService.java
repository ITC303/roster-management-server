package com.project.cafeemployeemanagement.service;

import com.project.cafeemployeemanagement.constant.Constants;
import com.project.cafeemployeemanagement.exception.AppException;
import com.project.cafeemployeemanagement.model.Employee;
import com.project.cafeemployeemanagement.model.EmployeeShift;
import com.project.cafeemployeemanagement.model.LeaveRequest;
import com.project.cafeemployeemanagement.model.LeaveStatus;
import com.project.cafeemployeemanagement.payload.*;
import com.project.cafeemployeemanagement.repository.EmployeeShiftRepository;
import com.project.cafeemployeemanagement.repository.LeaveRequestRepository;
import com.project.cafeemployeemanagement.util.ModelMapper;
import com.project.cafeemployeemanagement.util.utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeaveRequestService {

    @Autowired
    LeaveRequestRepository leaveRequestRepository;

    @Autowired
    EmployeeShiftRepository employeeShiftRepository;

    @Autowired
    EmployeeService employeeService;

    @Autowired
    UtilsService utilsService;

    @Transactional
    public EmployeeLeaveInfoResponse loadLeaveRequestsOfEmployee(final long employeeId) {
        List<LeaveRequest> leaveRequests = leaveRequestRepository.findByEmployee(employeeId);
        EmployeeLeaveInfoResponse employeeLeaveInfoResponse = new EmployeeLeaveInfoResponse();

        int pendingLeaves = getNumberOfPendingLeaves(leaveRequests);
        List<EmployeeLeaveRequest> employeeLeaveRequests = getPendingLeaves(leaveRequests);
        employeeLeaveInfoResponse.setPendingLeave(pendingLeaves);
        employeeLeaveInfoResponse.setLeaveBalance(getAnnualLeaveBalanceOfEmployee(employeeId));
        employeeLeaveInfoResponse.setLeaveRequests(employeeLeaveRequests);

        return employeeLeaveInfoResponse;
    }

    private int getNumberOfPendingLeaves(List<LeaveRequest> leaveRequests) {
        return leaveRequests.stream()
                .filter( leaveRequest -> leaveRequest.getStatus() == LeaveStatus.LEAVE_PENDING)
                .mapToInt( leaveRequest -> (int) leaveRequest.getNumberOfOffDates())
                .sum();
    }

    private List<EmployeeLeaveRequest> getPendingLeaves(List<LeaveRequest> leaveRequests) {
        return leaveRequests.stream()
                .map( leaveRequest -> {
                    EmployeeLeaveRequest employeeLeaveRequest = new EmployeeLeaveRequest();
                    employeeLeaveRequest.setId(leaveRequest.getId());
                    employeeLeaveRequest.setFromDate(utils.formatDate(leaveRequest.getFromDate()));
                    employeeLeaveRequest.setToDate(utils.formatDate(leaveRequest.getToDate()));
                    employeeLeaveRequest.setNumberOfOffDates(leaveRequest.getNumberOfOffDates());
                    employeeLeaveRequest.setStatus(leaveRequest.getStatus().name());
                    return employeeLeaveRequest;
                }).collect(Collectors.toList());
    }

    public int getAnnualLeaveBalanceOfEmployee(final long employeeId) {
        int annualLeaveBalance;
        int totalWorkedHours = getTotalWorkedHours(employeeId);
        int usedLeaves = getUsedLeaves(employeeId);

        annualLeaveBalance = (totalWorkedHours / Constants.NUMBER_OF_WORKED_HOURS_FOR_AN_HOUR_LEAVE) - usedLeaves;
        return annualLeaveBalance < 0 ? 0 : annualLeaveBalance;
    }

    private int getUsedLeaves(final long employeeId) {
        int usedLeaves;
        List<LeaveRequest> leaveRequests = leaveRequestRepository.findByEmployeeAndStatus(employeeId, LeaveStatus.LEAVE_APPROVED);
        usedLeaves = leaveRequests.stream().mapToInt(leaveRequest -> (int) leaveRequest.getNumberOfOffDates()).sum();
        return usedLeaves;
    }

    private int getTotalWorkedHours(final long employeeId) {
        int totalWorkedHours;
        List<EmployeeShift> employeeShifts = employeeShiftRepository.findByEmployeeAndShift(employeeId, new Date());
        totalWorkedHours = employeeShifts.stream().mapToInt(employeeShift -> (int) employeeShift.getWorkedHours()).sum();
        return totalWorkedHours;
    }

    public void submitLeaveRequest(final SubmitLeaveRequest submitLeaveRequest) {
        Employee employee = employeeService.loadById(submitLeaveRequest.getEmployeeId());
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployee(employee);
        leaveRequest.setCreatedDate(new Date());
        leaveRequest.setFromDate(submitLeaveRequest.getFromDate());
        leaveRequest.setToDate(submitLeaveRequest.getToDate());
        leaveRequest.setNote(submitLeaveRequest.getNote());
        leaveRequest.setStatus(LeaveStatus.LEAVE_PENDING);

        if (leaveRequestRepository.save(leaveRequest) == null) {
            throw new AppException("Failed to submit leave request!");
        }
    }

    private void updateLeaveRequest(final UpdateLeaveRequest updateLeaveRequest, final LeaveStatus leaveStatus) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(updateLeaveRequest.getLeaveRequestId())
                .orElseThrow(() -> new AppException("Cannot find leave request!"));
        leaveRequest.setUpdatedManagerId(updateLeaveRequest.getManagerId());
        leaveRequest.setUpdatedDate(new Date());
        leaveRequest.setStatus(leaveStatus);
        leaveRequest.setNote(leaveRequest.getNote() + "; " + updateLeaveRequest.getNote());

        if (leaveRequestRepository.save(leaveRequest) == null) {
            throw new AppException("Failed to update leave request!");
        }
    }

    public void approveLeaveRequest(final UpdateLeaveRequest updateLeaveRequest) {
        updateLeaveRequest(updateLeaveRequest, LeaveStatus.LEAVE_APPROVED);
    }

    public void denyLeaveRequest(final UpdateLeaveRequest updateLeaveRequest) {
        updateLeaveRequest(updateLeaveRequest, LeaveStatus.LEAVE_DENIED);
    }

    @Transactional
    public List<LeaveRequestsResponse> loadEmployeesLeaveRequests(final long shopOwnerId) {
        List<LeaveRequest> leaveRequests = leaveRequestRepository.findByShopOwnerIdAndStatus(shopOwnerId, LeaveStatus.LEAVE_PENDING);
        return ModelMapper.mapLeaveRequestsToLeaveRequestsResponse(leaveRequests);
    }
}
