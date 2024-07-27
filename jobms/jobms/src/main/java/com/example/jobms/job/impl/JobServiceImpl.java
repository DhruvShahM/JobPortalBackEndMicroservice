package com.example.jobms.job.impl;


import com.example.jobms.job.Job;
import com.example.jobms.job.JobRepository;
import com.example.jobms.job.JobService;
import com.example.jobms.job.clients.CompanyClient;
import com.example.jobms.job.clients.ReviewClient;
import com.example.jobms.job.dto.JobDTO;
import com.example.jobms.job.external.Company;
import com.example.jobms.job.external.Review;
import com.example.jobms.mapper.JobMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class JobServiceImpl implements JobService {
    // private List<Job> jobs = new ArrayList<>();
    JobRepository jobRepository;

    @Autowired
    RestTemplate restTemplate;

    private CompanyClient companyClient;
    private ReviewClient reviewClient;
    int attempt=0;

    public JobServiceImpl(JobRepository jobRepository, CompanyClient companyClient,ReviewClient reviewClient) {
        this.jobRepository = jobRepository;
        this.companyClient=companyClient;
        this.reviewClient=reviewClient;
    }

    @Override
//    @Retry(name="companyBreaker",fallbackMethod = "companyBreakerFallback")
//    @CircuitBreaker(name="companyBreaker",fallbackMethod = "companyBreakerFallback")
    @RateLimiter(name="companyBreaker",fallbackMethod = "companyBreakerFallback")
//    public List<Job> findAll()
    public List<JobDTO> findAll() {
        System.out.println("Attempt"+ ++attempt);
        List<Job> jobs=jobRepository.findAll();
//        List<JobWithCompanyDTO> jobWithCompanyDTOS=new ArrayList<>();


//        for(Job job:jobs){
//            JobWithCompanyDTO jobWithCompanyDTO=new JobWithCompanyDTO();
//            jobWithCompanyDTO.setJob(job);
//
//            Company company=restTemplate.getForObject("http://localhost:8082/companies/"+job.getCompanyId(), Company.class);
//            jobWithCompanyDTO.setCompany(company);
//            jobWithCompanyDTOS.add(jobWithCompanyDTO);
//        }
//        return jobWithCompanyDTOS;


//        stream use
       return jobs.stream().map(this::convertToDto).collect(Collectors.toList());

//        REST TEMPLATE TUTORIALS
//   RestTemplate restTemplate=new RestTemplate();
//       Company company=restTemplate.getForObject("http://localhost:8082/companies/1", Company.class);
//        System.out.println("COMPANY"+company.getName());
//        System.out.println("COMPANY"+company.getId());
//        return jobRepository.findAll();
    }


    public List<String> companyBreakerFallback(Exception e){
        List<String> list=new ArrayList<>();
        list.add("Dummy");
        return list;
    }

    private JobDTO convertToDto(Job job){
//        RestTemplate restTemplate=new RestTemplate();


////        http://COMPANYMS => Service Name by the Eurkea Server
//            Company company=restTemplate.getForObject
//                    ("http://COMPANYMS:8082/companies/"+job.getCompanyId(), Company.class);

        Company company=companyClient.getCompany(job.getCompanyId());

            List<Review> reviews=reviewClient.getReviews(job.getCompanyId());

//       ResponseEntity<List<Review>> reviewResponse =restTemplate.exchange("http://REVIEWMS:8083/reviews?companyId=" + job.getCompanyId(), HttpMethod.GET,
//                    null,
//                    new ParameterizedTypeReference<List<Review>>() {
//                    });
//       List<Review> reviews=reviewResponse.getBody();

        JobDTO jobDTO = JobMapper.maptoJobWithCompanyDTO(job,company,reviews);
//            jobDTO.setCompany(company);
            return jobDTO;
    }

    @Override
    public void createJob(Job job) {
        jobRepository.save(job);
    }

    @Override
    public JobDTO getJobById(Long id) {
        Job job=jobRepository.findById(id).orElse(null);
        return convertToDto(job);
    }

    @Override
    public boolean deleteJobById(Long id) {
        try {
            jobRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean updateJob(Long id, Job updatedJob) {
        Optional<Job> jobOptional = jobRepository.findById(id);
        if (jobOptional.isPresent()) {
            Job job = jobOptional.get();
            job.setTitle(updatedJob.getTitle());
            job.setDescription(updatedJob.getDescription());
            job.setMinSalary(updatedJob.getMinSalary());
            job.setMaxSalary(updatedJob.getMaxSalary());
            job.setLocation(updatedJob.getLocation());
            jobRepository.save(job);
            return true;
        }
        return false;
    }
}