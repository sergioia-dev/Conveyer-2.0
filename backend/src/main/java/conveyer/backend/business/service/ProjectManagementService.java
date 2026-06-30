package conveyer.backend.business.service;

import conveyer.backend.DTO.CreateProjectDTO;
import conveyer.backend.DTO.EditProjectDTO;
import conveyer.backend.persistance.model.Project;
import conveyer.backend.persistance.model.User;

public interface ProjectManagementService {

  public Project createProject(CreateProjectDTO dto, User user);

  public Project editProject(EditProjectDTO dto, User user);
}
