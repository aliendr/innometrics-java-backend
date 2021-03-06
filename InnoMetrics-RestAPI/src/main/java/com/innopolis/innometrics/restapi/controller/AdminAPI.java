package com.innopolis.innometrics.restapi.controller;

import com.innopolis.innometrics.restapi.DTO.*;
import com.innopolis.innometrics.restapi.config.JwtToken;
import com.innopolis.innometrics.restapi.entitiy.MeasurementType;
import com.innopolis.innometrics.restapi.entitiy.Project;
import com.innopolis.innometrics.restapi.entitiy.Role;
import com.innopolis.innometrics.restapi.entitiy.User;
import com.innopolis.innometrics.restapi.exceptions.ValidationException;
import com.innopolis.innometrics.restapi.repository.MeasurementTypeRepository;
import com.innopolis.innometrics.restapi.repository.ProjectRepository;
import com.innopolis.innometrics.restapi.repository.RoleRepository;
import com.innopolis.innometrics.restapi.service.AdminService;
import com.innopolis.innometrics.restapi.service.CategoryService;
import com.innopolis.innometrics.restapi.service.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Date;
import java.util.List;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.POST, RequestMethod.GET, RequestMethod.PUT})
@RequestMapping(value = "/V1/Admin", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminAPI {

    private static Logger LOG = LogManager.getLogger();

    @Autowired
    private JwtToken jwtTokenUtil;

    @Autowired
    RoleRepository roleService;

    @Autowired
    UserService userService;

    @Autowired
    ProjectRepository projectService;

    @Autowired
    AdminService adminService;

    @Autowired
    MeasurementTypeRepository measurementTypeService;

    @Autowired
    CategoryService categoryService;

    @GetMapping("/Role")
    public ResponseEntity<List<Role>> ListAllRoles(@RequestHeader String Token) {
        List<Role> lTemp = roleService.findAll();
        if (lTemp.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(lTemp, HttpStatus.OK);
    }

    @GetMapping("/Role/ById")
    public ResponseEntity<Role> GetRoleById(@RequestParam String RoleId, @RequestHeader String Token) {
        Role myRole = roleService.findByName(RoleId);
        if (myRole == null) {
            throw new ValidationException("The role doesn't exist");
        }

        return new ResponseEntity<>(myRole, HttpStatus.OK);
    }

    @PostMapping("/Role")
    public ResponseEntity<Role> CreateRole(@RequestParam String RoleId, @RequestParam String RoleDescription, @RequestHeader String Token) {

        if (roleService.findByName(RoleId) != null) {
            throw new ValidationException("The role already existed");
        }

        String UserName = jwtTokenUtil.getUsernameFromToken(Token);

        Role myRole = new Role();
        myRole.setName(RoleId);
        myRole.setDescription(RoleDescription);
        myRole.setCreatedby(UserName);
        myRole.setCreationdate(new Date());
        myRole.setIsactive("Y");

        myRole = roleService.save(myRole);

        return new ResponseEntity<>(myRole, HttpStatus.CREATED);
    }


    @PutMapping("/Role")
    public ResponseEntity<Role> UpdateRole(@RequestParam String RoleId, @RequestParam String RoleDescription, @RequestHeader String Token) {


        Role myRole = roleService.findByName(RoleId);
        if (myRole == null) {
            throw new ValidationException("The role doesn't exist");
        }
        String UserName = jwtTokenUtil.getUsernameFromToken(Token);

        myRole.setDescription(RoleDescription);
        myRole.setLastupdate(new Date());
        myRole.setUpdateby(UserName);


        myRole = roleService.save(myRole);

        return new ResponseEntity<>(myRole, HttpStatus.OK);
    }

    @PostMapping("/User")
    public ResponseEntity<UserRequest> CreateUser(@RequestBody UserRequest user, @RequestHeader(required = false) String Token) {
        if (user == null) {
            throw new ValidationException("Not enough data provided");
        }

        if (user.getEmail() == null || user.getName() == null || user.getSurname() == null || user.getPassword() == null) {
            throw new ValidationException("Not enough data provided");
        }

        if (userService.existsByEmail(user.getEmail())) {
            throw new ValidationException("Username already existed");
            //return new ResponseEntity<>("Username already existed", HttpStatus.FOUND);
        }

        String UserName = "API";

        if (Token != null) {
            UserName = jwtTokenUtil.getUsernameFromToken(Token);
        }

        User myUser = new User();
        myUser.setEmail(user.getEmail());
        //myUser.setPassword(new BCryptPasswordEncoder().encode(user.getPassword()));
        myUser.setPassword(user.getPassword());
        myUser.setName(user.getName());
        myUser.setSurname(user.getSurname());
        myUser.setCreationdate(new Date());
        myUser.setCreatedby(UserName);
        myUser.setIsactive("Y");

        userService.create(myUser);

        return new ResponseEntity<>(user, HttpStatus.CREATED);

    }

    @PutMapping("/User/UpdateStatus")
    public ResponseEntity<Boolean> UpdateUserStatus(@RequestParam String UserId, @RequestParam Boolean IsActive, @RequestHeader String Token) {
        if (UserId == null) {
            throw new ValidationException("Not enough data provided");
        }

        User myUser = userService.findByEmail(UserId);

        if (myUser == null) {
            throw new ValidationException("Username doesn't");
            //return new ResponseEntity<>("Username already existed", HttpStatus.FOUND);
        }

        String UserName = jwtTokenUtil.getUsernameFromToken(Token);

        myUser.setIsactive(IsActive ? "Y" : "N");
        myUser.setLastupdate(new Date());
        myUser.setUpdateby(UserName);

        Boolean response = userService.update(myUser) != null;

        return new ResponseEntity<>(response, HttpStatus.OK);

    }


    //Add project
    @PostMapping("/Project")
    public ResponseEntity<ProjectResponse> CreateProject(@RequestBody ProjectRequest project, @RequestHeader(required = false) String Token) {
        ProjectResponse response = adminService.CreateProject(project, Token);
        return ResponseEntity.ok(response);
    }


    @PutMapping("/Project")
    public ResponseEntity<ProjectResponse> updateProject(@RequestBody ProjectRequest project, @RequestHeader(required = true) String Token) {
        ProjectResponse response = adminService.updateProject(project, Token);
        return ResponseEntity.ok(response);
    }

    //Invite user in a project
    @PostMapping("/project/{ProjectName}")
    public ResponseEntity<Boolean> InviteUserProject(@PathVariable String ProjectName,
                                                     @RequestParam String UserEmail,
                                                     @RequestParam Boolean Manager,
                                                     @RequestHeader String Token) {
        if (ProjectName == null || UserEmail == null) {
            throw new ValidationException("Not enough data provided");
        }

        Project myProject = projectService.findByName(ProjectName);
        if (myProject == null) {
            throw new ValidationException("Project doesn't exist");
        }

        User myUser = userService.findByEmail(UserEmail);
        if (myUser == null) {
            throw new ValidationException("User doesn't exist");
        }

        //myProject.ge(name);
        // add user to manager or user list
        projectService.save(myProject);

        return new ResponseEntity<>(HttpStatus.CREATED);

    }


    @PostMapping("/MeasurementType")
    public ResponseEntity<MeasurementTypeResponse> CreateMeasurementType(@RequestBody MeasurementTypeRequest measurementType,
                                                                         @RequestHeader String Token) {

        if (measurementType == null) {
            throw new ValidationException("Not enough data provided");
        }

        if (measurementType.getLabel() == null || measurementType.getWeight() == null) {
            throw new ValidationException("Not enough data provided");
        }

        if (measurementTypeService.existsByLabel(measurementType.getLabel())) {
            throw new ValidationException("Measurement type already existed");
        }

        String UserName = jwtTokenUtil.getUsernameFromToken(Token);


        MeasurementType myType = new MeasurementType();
        myType.setLabel(measurementType.getLabel());
        myType.setDescription(measurementType.getDescription());
        myType.setWeight(measurementType.getWeight());
        myType.setOperation(measurementType.getOperation());
        myType.setScale(measurementType.getScale());
        myType.setIsactive("Y");
        myType.setCreatedby(UserName);
        myType.setCreationdate(new Date());

        measurementTypeService.save(myType);

        MeasurementTypeResponse response = new MeasurementTypeResponse(myType);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    @GetMapping("/Project")
    public ResponseEntity<ProjectListResponse> getActiveProjects() {
        ProjectListResponse response = adminService.getActiveProjects();
        return ResponseEntity.ok(response);
    }
    @GetMapping("/Users")
    public ResponseEntity<UserListResponse> getActiveUsers(@RequestParam(required = false) String ProjectId) {
        UserListResponse response = adminService.getActiveUsers(ProjectId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/Users/projects/{UserName}")
    public ResponseEntity<ProjectListResponse> getProjectsByUsername(@PathVariable String UserName,@RequestHeader String Token) {

        ProjectListResponse response = adminService.getProjectsByUsername(UserName);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/Classification/Category")
    public ResponseEntity<CategoryListResponse> getAllCategories(@RequestHeader(required = false) String Token) {

        if(Token == null) Token = "";
        CategoryListResponse response = categoryService.getAllCategories(Token);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/Classification/Category")
    public ResponseEntity<CategoryResponse> addCategory(@RequestBody CategoryRequest categoryRequest,
                                                        UriComponentsBuilder ucBuilder,
                                                        @RequestHeader(required = false) String Token) {

        if(Token == null) Token = "";
        CategoryResponse response = categoryService.addCategory(categoryRequest, Token);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/Classification/Category/{CategoryId}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Integer CategoryId, @RequestHeader(required = false) String Token) {

        if(Token == null) Token = "";
        CategoryResponse response = categoryService.getCategoryById(CategoryId, Token);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/Classification/Category")
    public ResponseEntity<CategoryResponse> UpdateCategory(@RequestBody CategoryRequest categoryRequest,
                                                           UriComponentsBuilder ucBuilder,
                                                           @RequestHeader(required = false) String Token) {

        if(Token == null) Token = "";
        CategoryResponse response = categoryService.UpdateCategory(categoryRequest, Token);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }



    @PostMapping("/Classification/App")
    public ResponseEntity<AppCategoryResponse> addAppCategory(@RequestBody AppCategoryRequest appCategoryRequest,
                                                              UriComponentsBuilder ucBuilder,
                                                              @RequestHeader(required = false) String Token) {


        if(Token == null) Token = "";
        AppCategoryResponse response = categoryService.addAppCategory(appCategoryRequest, Token);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/Classification/App/{AppId}")
    public ResponseEntity<AppCategoryResponse> getAppCategoryById(@PathVariable Integer AppId, @RequestHeader(required = false) String Token) {

        if(Token == null) Token = "";
        AppCategoryResponse response = categoryService.getAppCategoryById(AppId, Token);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/Classification/App")
    public ResponseEntity<AppCategoryResponse> UpdateAppCategory(@RequestBody AppCategoryRequest appCategoryRequest,
                                                                 UriComponentsBuilder ucBuilder,
                                                                 @RequestHeader(required = false) String Token) {

        if(Token == null) Token = "";
        AppCategoryResponse response = categoryService.UpdateAppCategory(appCategoryRequest, Token);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    //accept invitation

    //Load user from request

}
