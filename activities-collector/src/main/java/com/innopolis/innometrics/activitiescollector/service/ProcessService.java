package com.innopolis.innometrics.activitiescollector.service;

import com.innopolis.innometrics.activitiescollector.DTO.*;
import com.innopolis.innometrics.activitiescollector.entity.*;
import com.innopolis.innometrics.activitiescollector.entity.Process;
import com.innopolis.innometrics.activitiescollector.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@Transactional
public class ProcessService {

    @Autowired
    ProcessRepository processRepository;

    @Autowired
    MeasurementTypeRepository measurementTypeRepository;

    @Autowired
    ProcessMeasurementRepository processMeasurementRepository;

    public boolean CreateProcessReport(ProcessReport processReport, String UserName, Date CreationDate) {
        Process myProcess = new Process();
        //myProcess.setProcessID(processReport.getProcessID());
        myProcess.setEmail(processReport.getUserID());
        myProcess.setExecutable_name(processReport.getProcessName());
        myProcess.setIp_address(processReport.getIp_address());
        myProcess.setMac_address(processReport.getMac_address());
        myProcess.setPid(processReport.getPid());
        myProcess.setCollectedtime(processReport.getCollectedTime());
        myProcess.setOsversion(processReport.getOsversion());
        myProcess.setCreationdate(CreationDate);
        myProcess.setCreatedby(UserName);

        myProcess = processRepository.save(myProcess);

        for (MeasurementReport m : processReport.getMeasurementReportList()) {
            ProcessMeasurement myMeasurement = new ProcessMeasurement();
            myMeasurement.setProcess(myProcess);
            MeasurementType myType = new MeasurementType();
            try {
                myType = measurementTypeRepository.findByMeasurementtypeid(Integer.parseInt(m.getMeasurementTypeId()));
            } catch (Exception e) {
                myType = measurementTypeRepository.findByMeasurementtypeid(1);
            }

            if (myType == null) {
                //throw new ValidationException("The measurement type select does not exist");
            }
            myMeasurement.setMeasurementType(myType);
            myMeasurement.setValue(m.getValue());
            myMeasurement.setCaptureddate(m.getCapturedDate());
            myMeasurement.setCreationdate(CreationDate);
            myMeasurement.setCreatedby(UserName);

            myMeasurement = processMeasurementRepository.save(myMeasurement);

            myProcess.getProceMeasurements().add(myMeasurement);
        }

        return true;
    }

    public boolean DeleteActivity(Integer ProcessID, String UserName) {
        if (ProcessID == null || UserName == null) {
            //throw new ValidationException("Not enough data provided");
        }

        if (!processRepository.existsById(ProcessID)) {
            //throw new ValidationException("The activity doesn't exist");
        }

        processRepository.deleteById(ProcessID);

        return true;
    }

    public ProcessReportResponse getActivitiesByEmail(String UserName) {
        List<Process> myProcesses = processRepository.findByEmail(UserName);

        ProcessReportResponse response = new ProcessReportResponse();

        for (Process p : myProcesses) {
            ProcessReport myApp = new ProcessReport();
            myApp.setUserID(p.getEmail());
            myApp.setProcessName(p.getExecutable_name());
            myApp.setIp_address(p.getIp_address());
            myApp.setMac_address(p.getMac_address());

            for (ProcessMeasurement m : p.getProceMeasurements()) {
                MeasurementReport myMeasure = new MeasurementReport();
                myMeasure.setMeasurementTypeId(m.getMeasurementType().getMeasurementtypeid().toString());
                myMeasure.setValue(m.getValue());
                myMeasure.setAlternativeLabel(m.getMeasurementType().getLabel());
                myApp.getMeasurementReportList().add(myMeasure);
            }
            response.getProcessReports().add(myApp);
        }

        return response;
    }





}
