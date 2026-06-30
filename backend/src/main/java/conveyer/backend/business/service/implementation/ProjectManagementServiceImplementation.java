package conveyer.backend.business.service.implementation;

import org.springframework.stereotype.Service;

import conveyer.backend.DTO.CreateProjectDTO;
import conveyer.backend.DTO.EditProjectDTO;
import conveyer.backend.business.repository.ProjectRepository;
import conveyer.backend.business.service.ProjectManagementService;
import conveyer.backend.persistance.model.Project;
import conveyer.backend.persistance.model.User;

@Service
class ProjectManagementServiceImplementation implements ProjectManagementService {

  private final ProjectRepository projectRepository;

  public ProjectManagementServiceImplementation(ProjectRepository projectRepository) {
    this.projectRepository = projectRepository;
  }

  @Override
  public Project createProject(CreateProjectDTO dto, User user) {
    if (projectRepository.existsByNameAndUser(dto.name(), user)) {
      return null;
    }

    Project project = new Project();
    project.setName(dto.name());
    project.setDomain(dto.domain());
    project.setDescription(dto.description());
    project.setUser(user);
    projectRepository.save(project);

    return project;
  }

  @Override
  public Project editProject(EditProjectDTO dto, User user) {
    if (projectRepository.existsByNameAndUserAndIdNot(dto.name(), user, dto.id())) {
      return null;
    }

    Project project = projectRepository.findByIdAndUser(dto.id(), user).orElse(null);
    if (project == null) {
      return null;
    }

    project.setName(dto.name());
    project.setDomain(dto.domain());
    project.setDescription(dto.description());
    projectRepository.save(project);

    return project;
  }
}
